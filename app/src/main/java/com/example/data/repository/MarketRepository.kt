package com.example.data.repository

import com.example.data.api.*
import com.example.data.local.WatchlistDao
import com.example.data.model.Candle
import com.example.data.model.WatchlistItem
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MarketRepository(
    private val watchlistDao: WatchlistDao,
    private val keyManager: KeyManager
) {

    val watchlistStream: Flow<List<WatchlistItem>> = watchlistDao.getAll()

    suspend fun toggleWatchlist(symbol: String, name: String, type: String) {
        if (watchlistDao.exists(symbol)) {
            val list = watchlistDao.getAllList()
            val item = list.find { it.symbol == symbol }
            if (item != null) {
                watchlistDao.delete(item)
            }
        } else {
            // Fetch live initial price if valid API keys exist, otherwise fall back to base mock price
            var basePrice = getMockBasePrice(symbol)
            var baseChange = Random.nextDouble(-1.5, 2.0)

            val fhKey = keyManager.getApiKey("FINNHUB")
            val twKey = keyManager.getApiKey("TWELVEDATA")
            var fetched = false

            if (fhKey.isNotBlank()) {
                try {
                    val quote = RetrofitClient.finnhubService.getQuote(symbol, fhKey)
                    if (quote.c > 0) {
                        basePrice = quote.c
                        baseChange = quote.dp ?: baseChange
                        fetched = true
                    }
                } catch (e: Exception) {
                    // Ignore and move to next fallback
                }
            }

            if (!fetched && twKey.isNotBlank()) {
                try {
                    val series = RetrofitClient.twelveDataService.getTimeSeries(symbol, "1day", 2, twKey)
                    if (series.values != null && series.values.isNotEmpty()) {
                        val latestPrice = series.values[0].close.toDoubleOrNull()
                        if (latestPrice != null) {
                            basePrice = latestPrice
                            if (series.values.size >= 2) {
                                val prev = series.values[1].close.toDoubleOrNull()
                                if (prev != null && prev > 0) {
                                    baseChange = ((latestPrice - prev) / prev) * 100.0
                                }
                            }
                            fetched = true
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            watchlistDao.insert(
                WatchlistItem(
                    symbol = symbol,
                    name = name,
                    type = type,
                    price = basePrice,
                    change = baseChange,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Refresh currently pinned assets with real time API quotes
    suspend fun refreshWatchlistPrices() {
        val items = watchlistDao.getAllList()
        if (items.isEmpty()) return

        val fhKey = keyManager.getApiKey("FINNHUB")
        val twKey = keyManager.getApiKey("TWELVEDATA")

        for (item in items) {
            var updatedPrice = item.price
            var updatedChange = item.change
            var updated = false

            // Try Finnhub first
            if (fhKey.isNotBlank()) {
                try {
                    val quote = RetrofitClient.finnhubService.getQuote(item.symbol, fhKey)
                    if (quote.c > 0) {
                        updatedPrice = quote.c
                        updatedChange = quote.dp ?: item.change
                        updated = true
                    }
                } catch (e: Exception) {
                    // fall back
                }
            }

            // Try Twelve Data daily candles if not updated yet
            if (!updated && twKey.isNotBlank()) {
                try {
                    val series = RetrofitClient.twelveDataService.getTimeSeries(item.symbol, "1day", 2, twKey)
                    if (series.values != null && series.values.isNotEmpty()) {
                        val latestPrice = series.values[0].close.toDoubleOrNull()
                        if (latestPrice != null) {
                            updatedPrice = latestPrice
                            if (series.values.size >= 2) {
                                val prev = series.values[1].close.toDoubleOrNull()
                                if (prev != null && prev > 0) {
                                    updatedChange = ((latestPrice - prev) / prev) * 100.0
                                }
                            }
                            updated = true
                        }
                    }
                } catch (e: Exception) {
                    // fall back
                }
            }

            if (updated) {
                watchlistDao.insert(
                    item.copy(
                        price = updatedPrice,
                        change = updatedChange
                    )
                )
            }
        }
    }

    suspend fun isPinned(symbol: String): Boolean {
        return watchlistDao.exists(symbol)
    }

    // Universal symbol search supporting Twelve Data api & local database fallback
    suspend fun searchSymbols(query: String): List<WatchlistItem> {
        val twKey = keyManager.getApiKey("TWELVEDATA")
        if (twKey.isNotBlank()) {
            try {
                val res = RetrofitClient.twelveDataService.searchSymbols(query, twKey)
                if (res.status == "error") {
                    keyManager.markLimitReached("TWELVEDATA")
                } else if (res.data != null) {
                    return res.data.map {
                        WatchlistItem(
                            symbol = it.symbol,
                            name = it.name ?: it.symbol,
                            type = mapType(it.type),
                            price = getMockBasePrice(it.symbol),
                            change = Random.nextDouble(-2.0, 2.5)
                        )
                    }
                }
            } catch (e: Exception) {
                // fall back to mock
            }
        }

        // Return rich local mock list sorted by match
        return getMockSymbolList().filter {
            it.symbol.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
        }
    }

    // Fetch candles from Twelve Data, cached locally if rate limited
    suspend fun getCandles(symbol: String, interval: String): List<Candle> {
        val twKey = keyManager.getApiKey("TWELVEDATA")
        if (twKey.isNotBlank()) {
            try {
                // Map interval selector strings to Twelve Data friendly standards
                val mappedInterval = when (interval) {
                    "1m" -> "1min"
                    "5m" -> "5min"
                    "15m" -> "15min"
                    "1H" -> "1h"
                    "4H" -> "4h"
                    "Daily" -> "1day"
                    "Weekly" -> "1week"
                    else -> interval
                }

                val res = RetrofitClient.twelveDataService.getTimeSeries(
                    symbol = symbol,
                    interval = mappedInterval,
                    outputSize = 60,
                    apiKey = twKey
                )
                if (res.status == "error" || res.values == null) {
                    if (res.status == "error") {
                        keyManager.markLimitReached("TWELVEDATA")
                    }
                } else {
                    // Parse raw candle datetime strings (robust with fallback for day format like YYYY-MM-DD vs subday with clock)
                    val sdfSeconds = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                    val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                    val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

                    return res.values.mapNotNull { v ->
                        val date = try {
                            if (v.datetime.contains(" ")) {
                                if (v.datetime.count { it == ':' } == 2) {
                                    sdfSeconds.parse(v.datetime)?.time
                                } else {
                                    sdfFull.parse(v.datetime)?.time
                                }
                            } else {
                                sdfDay.parse(v.datetime)?.time
                            } ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        Candle(
                            open = v.open.toDoubleOrNull() ?: return@mapNotNull null,
                            high = v.high.toDoubleOrNull() ?: return@mapNotNull null,
                            low = v.low.toDoubleOrNull() ?: return@mapNotNull null,
                            close = v.close.toDoubleOrNull() ?: return@mapNotNull null,
                            volume = v.volume?.toDoubleOrNull() ?: 1000.0,
                            timestamp = date
                        )
                    }.sortedBy { it.timestamp }
                }
            } catch (e: Exception) {
                // fallback to mock on error
            }
        }
        return generateMockCandles(symbol, interval)
    }

    // News sentiment query
    suspend fun getTradingNews(): List<NewsArticle> {
        val newsKey = keyManager.getApiKey("NEWSAPI")
        if (newsKey.isNotBlank()) {
            try {
                val res = RetrofitClient.newsApiService.getNews(apiKey = newsKey)
                if (res.status == "ok" && !res.articles.isNullOrEmpty()) {
                    return res.articles
                } else if (res.status == "error") {
                    keyManager.markLimitReached("NEWSAPI")
                }
            } catch (e: Exception) {
                // Fall down
            }
        }
        return getMockNewsArticles()
    }

    // Finnhub Company Info
    suspend fun getCompanyDetails(symbol: String): FinnhubCompanyProfile {
        val finnhubKey = keyManager.getApiKey("FINNHUB")
        if (finnhubKey.isNotBlank()) {
            try {
                return RetrofitClient.finnhubService.getCompanyProfile(symbol, finnhubKey)
            } catch (e: Exception) {
                // Fall
            }
        }
        return FinnhubCompanyProfile(
            name = "Interactive Asset - $symbol",
            ticker = symbol,
            exchange = "Universal Markets",
            ipo = "2015-05-18",
            marketCapitalization = 184512.4,
            shareOutstanding = 512.9,
            weburl = "https://tradeaipro.app",
            finnhubIndustry = "Trading & Wealth"
        )
    }

    // Get current quote price
    suspend fun getQuotePrice(symbol: String): Double {
        val fhKey = keyManager.getApiKey("FINNHUB")
        if (fhKey.isNotBlank()) {
            try {
                val quote = RetrofitClient.finnhubService.getQuote(symbol, fhKey)
                if (quote.c > 0) return quote.c
            } catch (e: Exception) {
                // Fall back
            }
        }
        return getMockBasePrice(symbol)
    }

    // Helper maps
    private fun mapType(rawType: String): String {
        val t = rawType.uppercase()
        return when {
            t.contains("DIGITAL") || t.contains("CRYPTO") -> "CRYPTO"
            t.contains("STOCK") || t.contains("EQUITY") -> "STOCK"
            t.contains("CURRENCY") || t.contains("FOREX") -> "FOREX"
            t.contains("COMMODITY") || t.contains("METAL") -> "COMMODITY"
            else -> "INDEX"
        }
    }

    private fun getMockBasePrice(symbol: String): Double {
        val clean = symbol.uppercase()
        return when {
            clean.contains("BTC") -> 68420.5
            clean.contains("ETH") -> 3740.2
            clean.contains("SOL") -> 148.5
            clean.contains("AAPL") -> 183.92
            clean.contains("TSLA") -> 177.46
            clean.contains("NVDA") -> 920.15
            clean.contains("EUR/USD") -> 1.0845
            clean.contains("GBP/USD") -> 1.2582
            clean.contains("XAU") || clean.contains("GOLD") -> 2320.10
            clean.contains("WTI") || clean.contains("OIL") -> 78.40
            else -> Random.nextDouble(50.0, 500.0)
        }
    }

    private fun getMockSymbolList(): List<WatchlistItem> {
        return listOf(
            WatchlistItem("BTC/USD", "Bitcoin", "CRYPTO", 68420.5, 1.45),
            WatchlistItem("ETH/USD", "Ethereum", "CRYPTO", 3740.2, -0.85),
            WatchlistItem("SOL/USD", "Solana", "CRYPTO", 148.5, 3.20),
            WatchlistItem("EUR/USD", "Euro / US Dollar", "FOREX", 1.0845, 0.12),
            WatchlistItem("GBP/USD", "British Pound / US Dollar", "FOREX", 1.2582, -0.05),
            WatchlistItem("XAU/USD", "Gold Spot", "COMMODITY", 2320.10, 0.78),
            WatchlistItem("WTI/USD", "Crude Oil WTI", "COMMODITY", 78.40, -1.25),
            WatchlistItem("AAPL", "Apple Inc.", "STOCK", 183.92, 0.45),
            WatchlistItem("NVDA", "NVIDIA Corporation", "STOCK", 920.15, 4.12),
            WatchlistItem("TSLA", "Tesla Inc.", "STOCK", 177.46, -2.30),
            WatchlistItem("SPX", "S&P 500", "INDEX", 5120.40, 0.25)
        )
    }

    private fun generateMockCandles(symbol: String, interval: String): List<Candle> {
        val candles = mutableListOf<Candle>()
        var basePrice = getMockBasePrice(symbol)
        val numCandles = 60
        var currentT = System.currentTimeMillis() - (numCandles * 60000)

        val tStep = when (interval) {
            "1m" -> 60000L
            "5m" -> 300000L
            "15m" -> 900000L
            "1h" -> 3600000L
            "4h" -> 14400000L
            else -> 86400000L
        }

        // Seed generator for same symbol to have consistent charts
        val rand = Random(symbol.hashCode() + interval.hashCode())

        for (i in 0 until numCandles) {
            val changePercent = rand.nextDouble(-0.015, 0.016)
            val move = basePrice * changePercent
            val open = basePrice
            val close = basePrice + move

            // Extreme high/low spike
            val high = maxOf(open, close) + (basePrice * rand.nextDouble(0.0, 0.008))
            val low = minOf(open, close) - (basePrice * rand.nextDouble(0.0, 0.008))
            val volume = rand.nextDouble(500.0, 5000.0)

            candles.add(
                Candle(
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    timestamp = currentT
                )
            )
            basePrice = close
            currentT += tStep
        }
        return candles
    }

    private fun getMockNewsArticles(): List<NewsArticle> {
        return listOf(
            NewsArticle(
                source = NewsSource("1", "TradeAI Analysts"),
                author = "M4DI~UciH4",
                title = "Crypto Market Intelligence: Bullish Patterns Form on Key Majors",
                description = "Our proprietary AI signals indicate strong consolidation in BTC/USD and ETH/USD, signaling an impending breakout on the 4H timeframe.",
                url = "https://tradeaipro.app/articles/1",
                urlToImage = null,
                publishedAt = "2026-06-22T10:00:00Z",
                content = "Bitcoin is consolidating near key order blocks. ICT volume profiles display fair value gaps that have been filled successfully, which points to a bullish continuation."
            ),
            NewsArticle(
                source = NewsSource("2", "Global Forex News"),
                author = "Market Eye",
                title = "US Dollar Under Pressure Ahead of Fed Economic Outlook Report",
                description = "The DXY index undergoes negative sentiment test as MACD indicates bearish crossover, pushing EUR/USD above 1.0850 support level.",
                url = "https://tradeaipro.app/articles/2",
                urlToImage = null,
                publishedAt = "2026-06-22T08:30:00Z",
                content = "Forex markets react as retail indicators reach extreme overbought thresholds in major USD pairings."
            ),
            NewsArticle(
                source = NewsSource("3", "Commodity Watch"),
                author = "Metal Trader",
                title = "Gold Spot (XAU/USD) Reclaims Key Resistance Zone Amid Inflation Woes",
                description = "Gold spikes to $2325 as EMA 9 crosses above EMA 21 on the Daily chart. Bollinger Bands begin expanding, signifying massive volatility inflow.",
                url = "https://tradeaipro.app/articles/3",
                urlToImage = null,
                publishedAt = "2026-06-22T07:12:00Z",
                content = "The precious metal reclaims vital support boundaries as investors seek refuge from stock equities volatility."
            ),
            NewsArticle(
                source = NewsSource("4", "Stock Sentinel"),
                author = "WallStreet Pro",
                title = "NVIDIA (NVDA) Rebounds Strongly as Order Block Support Holds Securely",
                description = "Nvidia rebounds from $900 as buyers defend the primary bullish order block. RSI rebounds from 30 oversold territory, signaling clean scalping buy entries.",
                url = "https://tradeaipro.app/articles/4",
                urlToImage = null,
                publishedAt = "2026-06-21T21:00:00Z",
                content = "Visual computing giant continues to show technical strength as tech sector index experiences massive capital flows."
            )
        )
    }
}
