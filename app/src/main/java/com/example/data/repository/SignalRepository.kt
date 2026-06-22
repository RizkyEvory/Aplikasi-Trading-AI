package com.example.data.repository

import com.example.data.local.ScalpSignalDao
import com.example.data.model.ScalpSignal
import com.example.engine.SignalEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class SignalRepository(
    private val signalDao: ScalpSignalDao,
    private val marketRepository: MarketRepository
) {

    val allSignalsStream: Flow<List<ScalpSignal>> = signalDao.getAll()

    suspend fun clearSignals() {
        signalDao.clear()
    }

    // Refresh signals for all symbols currently pinned to the watchlist
    suspend fun refreshSignalsForWatchlist() {
        val watchlist = marketRepository.watchlistStream.first() ?: return
        if (watchlist.isEmpty()) {
            // Seed a default symbol signal to ensure there is exciting initial data in first launch!
            generateAndSaveMockSignals()
            return
        }

        for (item in watchlist) {
            // Scan modern liquid scalping timeframes (1m, 5m, 15m)
            val intervals = listOf("5m", "15m")
            for (interval in intervals) {
                try {
                    val candles = marketRepository.getCandles(item.symbol, interval)
                    if (candles.isNotEmpty()) {
                        val generated = SignalEngine.generateSignals(
                            symbol = item.symbol,
                            name = item.name,
                            candles = candles,
                            timeframe = interval
                        )
                        for (sig in generated) {
                            // Check duplication (same symbol, timeframe & timestamp within 60s)
                            val existing = signalDao.getAllList()
                            val duplicate = existing.any {
                                it.symbol == sig.symbol &&
                                        it.timeframe == sig.timeframe &&
                                        Math.abs(it.timestamp - sig.timestamp) < 60000
                            }
                            if (!duplicate) {
                                signalDao.insert(sig)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fail silently
                }
            }

            // Update open signals status based on latest price
            updateOpenSignalsStatus(item.symbol)
        }
    }

    private suspend fun updateOpenSignalsStatus(symbol: String) {
        val currentPrice = marketRepository.getQuotePrice(symbol)
        val allSignals = signalDao.getAllList()
        val openSignals = allSignals.filter { it.symbol == symbol && it.status == "OPEN" }

        for (sig in openSignals) {
            if (sig.direction == "BUY") {
                if (currentPrice >= sig.takeProfit) {
                    signalDao.insert(sig.copy(status = "WIN"))
                } else if (currentPrice <= sig.stopLoss) {
                    signalDao.insert(sig.copy(status = "LOSS"))
                }
            } else if (sig.direction == "SELL") {
                if (currentPrice <= sig.takeProfit) {
                    signalDao.insert(sig.copy(status = "WIN"))
                } else if (currentPrice >= sig.stopLoss) {
                    signalDao.insert(sig.copy(status = "LOSS"))
                }
            }
        }
    }

    suspend fun generateAndSaveMockSignals() {
        // Generates nice default historical mock signals to display if DB is completely empty
        val existing = signalDao.getAllList()
        if (existing.isNotEmpty()) return

        val items = listOf(
            ScalpSignal(
                symbol = "BTC/USD",
                name = "Bitcoin",
                direction = "BUY",
                timeframe = "5m",
                entryPrice = 68420.0,
                takeProfit = 69120.0,
                stopLoss = 67890.0,
                confidence = 90.0,
                triggerIndication = "EMA 9 cross EMA 21 + RSI Oversold (< 30) + ICT Bullish FVG",
                timestamp = System.currentTimeMillis() - 300000,
                status = "OPEN"
            ),
            ScalpSignal(
                symbol = "ETH/USD",
                name = "Ethereum",
                direction = "SELL",
                timeframe = "15m",
                entryPrice = 3740.0,
                takeProfit = 3680.0,
                stopLoss = 3795.0,
                confidence = 82.0,
                triggerIndication = "MACD Crossdown + RSI Overbought (> 70)",
                timestamp = System.currentTimeMillis() - 1200000,
                status = "WIN"
            ),
            ScalpSignal(
                symbol = "NVDA",
                name = "NVIDIA Corporation",
                direction = "BUY",
                timeframe = "1m",
                entryPrice = 920.0,
                takeProfit = 932.0,
                stopLoss = 912.0,
                confidence = 100.0,
                triggerIndication = "Bollinger Underband Touch + ICT Bullish Order Block",
                timestamp = System.currentTimeMillis() - 1800000,
                status = "OPEN"
            ),
            ScalpSignal(
                symbol = "XAU/USD",
                name = "Gold Spot",
                direction = "BUY",
                timeframe = "5m",
                entryPrice = 2320.0,
                takeProfit = 2335.0,
                stopLoss = 2310.0,
                confidence = 75.0,
                triggerIndication = "EMA cross + RSI oversold",
                timestamp = System.currentTimeMillis() - 7200000,
                status = "LOSS"
            )
        )

        for (item in items) {
            signalDao.insert(item)
        }
    }
}
