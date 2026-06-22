package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.data.model.WatchlistItem
import com.example.data.model.PriceTick
import com.example.engine.SignalEngine
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.viewmodel.MarketViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.BorderStroke
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(viewModel: MarketViewModel) {
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val selectedName by viewModel.selectedAssetName.collectAsState()
    val candles by viewModel.chartCandles.collectAsState()
    val realTimeTicks by viewModel.realTimeTicks.collectAsState()
    val isLoading by viewModel.isLoadingChart.collectAsState()
    val selectedInterval by viewModel.selectedInterval.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    
    var chartTabState by remember { mutableStateOf(1) } // 0 = Technicals & Candles, 1 = Live Feed

    // Config indicator toggles
    val showEma by viewModel.showEMA.collectAsState()
    val showRsi by viewModel.showRSI.collectAsState()
    val showMacd by viewModel.showMACD.collectAsState()
    val showBb by viewModel.showBB.collectAsState()

    var searchQueryInput by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var showSearchDialog by remember { mutableStateOf(false) }

    val intervals = listOf("1m", "5m", "15m", "1H", "4H", "Daily", "Weekly")

    val isPinned = watchlist.any { it.symbol == selectedSymbol }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedSymbol,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = selectedName,
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                },
                actions = {
                    // Watchlist pin / unpin button
                    IconButton(
                        onClick = {
                            val matching = watchlist.find { it.symbol == selectedSymbol }
                            if (matching != null) {
                                viewModel.toggleWatchlist(matching)
                            } else {
                                viewModel.toggleWatchlist(
                                    WatchlistItem(
                                        symbol = selectedSymbol,
                                        name = selectedName,
                                        type = "STOCK"
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Pin Symbol",
                            tint = Gold
                        )
                    }
                    // Universal Search Button
                    IconButton(
                        onClick = { showSearchDialog = true },
                        modifier = Modifier.testTag("search_asset_button")
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search market", tint = Gold)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Beautiful Segmented Tab switcher for chart view mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFF140D25), shape = RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Real-Time Feed (Finnhub)", "Technicals & Candles").forEachIndexed { index, label ->
                    val isSelected = (index == 0 && chartTabState == 1) || (index == 1 && chartTabState == 0)
                    Button(
                        onClick = { chartTabState = if (index == 0) 1 else 0 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Gold else Color.Transparent,
                            contentColor = if (isSelected) Color.Black else Color.Gray
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (chartTabState == 0) {
                // Timeframes selector
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(intervals) { tf ->
                        val isSelected = tf == selectedInterval
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) Gold else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.selectInterval(tf) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tf,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Technical Indicators toggles row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Indicators:", fontSize = 12.sp, color = Color.Gray)

                    IndicatorPill(name = "EMA 9/21", active = showEma) { viewModel.toggleEMA() }
                    IndicatorPill(name = "Bollinger", active = showBb) { viewModel.toggleBB() }
                    IndicatorPill(name = "RSI", active = showRsi) { viewModel.toggleRSI() }
                    IndicatorPill(name = "MACD", active = showMacd) { viewModel.toggleMACD() }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold)
                }
            } else if (candles.isEmpty() && chartTabState == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No Chart Data Available. Check API logs.", color = Color.Gray)
                }
            } else {
                if (chartTabState == 1) {
                    // Render premium interactive Real-Time Tick Chart (matching recharts look and feel)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        InteractiveLiveTickChart(
                            ticks = realTimeTicks,
                            symbol = selectedSymbol,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }
                } else {
                    // Interactive Chart Canvas
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        TradeCandlestickCanvas(
                            candles = candles,
                            showEma = showEma,
                            showBb = showBb,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 16.dp)
                        )
                    }

                    // Split RSI / MACD visualizers if active
                    if (showRsi) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.4f)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            RsiCanvas(candles = candles, modifier = Modifier.fillMaxSize().padding(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Interactive Search Dialog
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text(text = "Search Markets (Stocks/Forex/Crypto)", color = Gold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQueryInput,
                        onValueChange = {
                            searchQueryInput = it
                            viewModel.onSearchQueryChanged(it)
                        },
                        placeholder = { Text(text = "Enter symbol, e.g. BTC, ETH, AAPL") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("search_input_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            focusedLabelColor = Gold
                        )
                    )

                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Gold)
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Type search keyword...", color = Color.Gray)
                        }
                    } else {
                        Box(modifier = Modifier.height(250.dp)) {
                            androidx.compose.foundation.lazy.LazyColumn {
                                items(searchResults) { result ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectSymbol(result.symbol, result.name)
                                                showSearchDialog = false
                                            }
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = result.symbol,
                                                fontWeight = FontWeight.Bold,
                                                color = Gold
                                            )
                                            Text(
                                                text = result.name,
                                                fontSize = 11.sp,
                                                color = Color.LightGray
                                            )
                                        }
                                        Badge(containerColor = Gold, contentColor = Color.Black) {
                                            Text(text = result.type, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    HorizontalDivider(color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text(text = "Close", color = Gold)
                }
            }
        )
    }
}

@Composable
fun IndicatorPill(name: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (active) Gold.copy(alpha = 0.25f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (active) Gold else Color.Gray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = name,
            color = if (active) Gold else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TradeCandlestickCanvas(
    candles: List<Candle>,
    showEma: Boolean,
    showBb: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sizeH = size.height
        val sizeW = size.width

        val minPrice = candles.minOf { it.low }
        val maxPrice = candles.maxOf { it.high }
        val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else (maxPrice - minPrice)

        val paddingOffset = priceRange * 0.05
        val yMin = minPrice - paddingOffset
        val yMax = maxPrice + paddingOffset
        val yRange = yMax - yMin

        val numCandles = candles.size
        val candleWidth = sizeW / numCandles
        val wickWidth = 2f

        // 1. Draw Bollinger Bands first in the background
        if (showBb && numCandles >= 20) {
            val (bbUpper, bbMid, bbLower) = SignalEngine.calculateBollingerBands(candles, 20, 2.0)
            val fillPath = androidx.compose.ui.graphics.Path()

            // Construct fill region
            var first = true
            for (i in 0 until numCandles) {
                if (bbUpper[i] == 0.0 || bbLower[i] == 0.0) continue
                val x = (i * candleWidth) + (candleWidth / 2)
                val yHigh = sizeH - (((bbUpper[i] - yMin) / yRange) * sizeH).toFloat()

                if (first) {
                    fillPath.moveTo(x, yHigh)
                    first = false
                } else {
                    fillPath.lineTo(x, yHigh)
                }
            }

            for (i in (numCandles - 1) downTo 0) {
                if (bbUpper[i] == 0.0 || bbLower[i] == 0.0) continue
                val x = (i * candleWidth) + (candleWidth / 2)
                val yLow = sizeH - (((bbLower[i] - yMin) / yRange) * sizeH).toFloat()
                fillPath.lineTo(x, yLow)
            }

            fillPath.close()
            drawPath(
                path = fillPath,
                color = Gold.copy(alpha = 0.07f)
            )

            // Draw line upper and lower
            for (i in 0 until numCandles - 1) {
                if (bbUpper[i] == 0.0 || bbUpper[i + 1] == 0.0) continue
                val x1 = (i * candleWidth) + (candleWidth / 2)
                val y1 = sizeH - (((bbUpper[i] - yMin) / yRange) * sizeH).toFloat()
                val x2 = ((i + 1) * candleWidth) + (candleWidth / 2)
                val y2 = sizeH - (((bbUpper[i + 1] - yMin) / yRange) * sizeH).toFloat()

                val xLower1 = (i * candleWidth) + (candleWidth / 2)
                val yLower1 = sizeH - (((bbLower[i] - yMin) / yRange) * sizeH).toFloat()
                val xLower2 = ((i + 1) * candleWidth) + (candleWidth / 2)
                val yLower2 = sizeH - (((bbLower[i + 1] - yMin) / yRange) * sizeH).toFloat()

                drawLine(
                    color = Gold.copy(alpha = 0.35f),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 1.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                drawLine(
                    color = Gold.copy(alpha = 0.35f),
                    start = Offset(xLower1, yLower1),
                    end = Offset(xLower2, yLower2),
                    strokeWidth = 1.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }

        // 2. Draw candlestick bars
        for (i in 0 until numCandles) {
            val candle = candles[i]
            val x = i * candleWidth

            val openY = sizeH - (((candle.open - yMin) / yRange) * sizeH).toFloat()
            val closeY = sizeH - (((candle.close - yMin) / yRange) * sizeH).toFloat()
            val highY = sizeH - (((candle.high - yMin) / yRange) * sizeH).toFloat()
            val lowY = sizeH - (((candle.low - yMin) / yRange) * sizeH).toFloat()

            val isBullish = candle.close >= candle.open
            val candleColor = if (isBullish) Color(0xFF4CAF50) else Color(0xFFF44336)

            // Draw wick
            drawLine(
                color = candleColor,
                start = Offset(x + (candleWidth / 2), highY),
                end = Offset(x + (candleWidth / 2), lowY),
                strokeWidth = wickWidth
            )

            // Draw body
            val rectHeight = Math.abs(openY - closeY).coerceAtLeast(2f)
            val rectTop = minOf(openY, closeY)
            val rectW = (candleWidth * 0.75f).coerceAtLeast(3f)

            drawRect(
                color = candleColor,
                topLeft = Offset(x + (candleWidth * 0.125f), rectTop),
                size = Size(rectW, rectHeight)
            )
        }

        // 3. Draw EMA 9 / EMA 21 overlay lines
        if (showEma && numCandles >= 21) {
            val ema9 = SignalEngine.calculateEMA(candles, 9)
            val ema21 = SignalEngine.calculateEMA(candles, 21)

            for (i in 0 until numCandles - 1) {
                if (ema9[i] == 0.0 || ema9[i + 1] == 0.0) continue
                val x1 = (i * candleWidth) + (candleWidth / 2)
                val y9_1 = sizeH - (((ema9[i] - yMin) / yRange) * sizeH).toFloat()
                val x2 = ((i + 1) * candleWidth) + (candleWidth / 2)
                val y9_2 = sizeH - (((ema9[i + 1] - yMin) / yRange) * sizeH).toFloat()

                val y21_1 = sizeH - (((ema21[i] - yMin) / yRange) * sizeH).toFloat()
                val y21_2 = sizeH - (((ema21[i + 1] - yMin) / yRange) * sizeH).toFloat()

                // EMA 9 (Gold)
                drawLine(
                    color = GoldLight,
                    start = Offset(x1, y9_1),
                    end = Offset(x2, y9_2),
                    strokeWidth = 3f
                )

                // EMA 21 (Teal/Silver)
                drawLine(
                    color = Color(0xFF00B0FF),
                    start = Offset(x1, y21_1),
                    end = Offset(x2, y21_2),
                    strokeWidth = 3f
                )
            }
        }
    }
}

@Composable
fun RsiCanvas(candles: List<Candle>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizeH = size.height
        val sizeW = size.width
        val numCandles = candles.size
        val blockW = sizeW / numCandles

        // Draw dotted lines for 30 and 70 levels
        val y30 = sizeH - ((30f / 100f) * sizeH)
        val y70 = sizeH - ((70f / 100f) * sizeH)

        drawLine(
            color = Color.Red.copy(alpha = 0.5f),
            start = Offset(0f, y70),
            end = Offset(sizeW, y70),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        drawLine(
            color = Color.Green.copy(alpha = 0.5f),
            start = Offset(0f, y30),
            end = Offset(sizeW, y30),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Draw RSI curve
        val rsi = SignalEngine.calculateRSI(candles, 14)
        for (i in 0 until numCandles - 1) {
            val x1 = (i * blockW) + (blockW / 2)
            val rsiVal1 = rsi[i]
            val y_1 = sizeH - ((rsiVal1.toFloat() / 100f) * sizeH)

            val x2 = ((i + 1) * blockW) + (blockW / 2)
            val rsiVal2 = rsi[i + 1]
            val y_2 = sizeH - ((rsiVal2.toFloat() / 100f) * sizeH)

            drawLine(
                color = Gold,
                start = Offset(x1, y_1),
                end = Offset(x2, y_2),
                strokeWidth = 3.5f
            )
        }
    }
}

@Composable
fun InteractiveLiveTickChart(
    ticks: List<com.example.data.model.PriceTick>,
    symbol: String,
    modifier: Modifier = Modifier
) {
    if (ticks.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold)
        }
        return
    }

    val prices = ticks.map { it.price }
    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 1.0
    val range = if (maxPrice - minPrice == 0.0) 1.0 else (maxPrice - minPrice)

    // Add 10% vertical padding so chart lines don't touch the top and bottom borders
    val padPrice = range * 0.1
    val yMin = (minPrice - padPrice).coerceAtLeast(0.0)
    val yMax = maxPrice + padPrice
    val yRange = yMax - yMin

    // 1. Calculate Live Moving Average (9-period Simple Moving Average)
    val periodMA = 9
    val liveMA = remember(ticks) {
        val ma = MutableList(ticks.size) { 0.0 }
        for (i in ticks.indices) {
            if (i >= periodMA - 1) {
                var sum = 0.0
                for (j in 0 until periodMA) {
                    sum += ticks[i - j].price
                }
                ma[i] = sum / periodMA
            } else {
                ma[i] = ticks[i].price
            }
        }
        ma
    }

    // 2. Calculate Live RSI (7-period Relative Strength Index on real-time Tick Feed)
    val periodRSI = 7
    val liveRSI = remember(ticks) {
        val rsi = MutableList(ticks.size) { 50.0 }
        if (ticks.size >= periodRSI + 1) {
            var avgGain = 0.0
            var avgLoss = 0.0
            for (i in 1..periodRSI) {
                val change = ticks[i].price - ticks[i - 1].price
                if (change > 0) avgGain += change else avgLoss += -change
            }
            avgGain /= periodRSI
            avgLoss /= periodRSI

            if (avgLoss == 0.0) {
                rsi[periodRSI] = 100.0
            } else {
                val rs = avgGain / avgLoss
                rsi[periodRSI] = 100.0 - (100.0 / (1.0 + rs))
            }

            for (i in (periodRSI + 1) until ticks.size) {
                val change = ticks[i].price - ticks[i - 1].price
                val gain = if (change > 0) change else 0.0
                val loss = if (change < 0) -change else 0.0

                avgGain = (avgGain * (periodRSI - 1) + gain) / periodRSI
                avgLoss = (avgLoss * (periodRSI - 1) + loss) / periodRSI

                if (avgLoss == 0.0) {
                    rsi[i] = 100.0
                } else {
                    val rs = avgGain / avgLoss
                    rsi[i] = 100.0 - (100.0 / (1.0 + rs))
                }
            }
        }
        rsi
    }

    // 3. Scalping Signals based on fast price/MA breakout crossovers & RSI momentum verification
    val scalpingSignals = remember(ticks, liveMA, liveRSI) {
        val sigs = MutableList<String?>(ticks.size) { null }
        for (i in 1 until ticks.size) {
            val price = ticks[i].price
            val prevPrice = ticks[i - 1].price
            val maVal = liveMA[i]
            val prevMaVal = liveMA[i - 1]
            val rsiVal = liveRSI[i]

            val crossUp = price > maVal && prevPrice <= prevMaVal
            val crossDown = price < maVal && prevPrice >= prevMaVal

            if (crossUp && rsiVal > 45.0) {
                sigs[i] = "BUY"
            } else if (crossDown && rsiVal < 55.0) {
                sigs[i] = "SELL"
            }
        }
        sigs
    }

    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    var hoverOffset by remember { mutableStateOf<Offset?>(null) }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Live pulsing banner with indicators legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing green circle
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4CAF50), shape = RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FINNHUB SCALPER FEED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    // Legend pills
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2196F3).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("MA(9)", color = Color(0xFF2196F3), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Gold.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("RSI(7)", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Live: $${"%.4f".format(ticks.last().price)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0F0B1E), shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                    .pointerInput(ticks) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val stepX = size.width / (ticks.size - 1).coerceAtLeast(1)
                                val idx = (offset.x / stepX).roundToInt().coerceIn(0, ticks.size - 1)
                                hoverIndex = idx
                                hoverOffset = offset
                            },
                            onDragEnd = {
                                hoverIndex = null
                                hoverOffset = null
                            },
                            onDragCancel = {
                                hoverIndex = null
                                hoverOffset = null
                            }
                        ) { change, _ ->
                            val stepX = size.width / (ticks.size - 1).coerceAtLeast(1)
                            val idx = (change.position.x / stepX).roundToInt().coerceIn(0, ticks.size - 1)
                            hoverIndex = idx
                            hoverOffset = change.position
                        }
                    }
            ) {
                // Background Area, Line, Grid paint
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val sizeW = size.width
                    val sizeH = size.height

                    val pointsCount = ticks.size
                    val stepX = sizeW / (pointsCount - 1).coerceAtLeast(1)

                    // 1. Draw horizontal grid lines like recharts
                    val gridLineCount = 4
                    for (i in 0..gridLineCount) {
                        val yGrid = (sizeH / gridLineCount) * i
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, yGrid),
                            end = Offset(sizeW, yGrid),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                        )
                    }

                    // 2. Map coordinates for line path
                    val points = ticks.mapIndexed { idx, tick ->
                        val x = idx * stepX
                        val y = sizeH - (((tick.price - yMin) / yRange) * sizeH).toFloat()
                        Offset(x, y)
                    }

                    // Map coordinates for Moving over average values
                    val maPoints = liveMA.mapIndexed { idx, maValue ->
                        val x = idx * stepX
                        val y = sizeH - (((maValue - yMin) / yRange) * sizeH).toFloat()
                        Offset(x, y)
                    }

                    // 3. Draw gradient area fill under the price curve
                    if (points.isNotEmpty()) {
                        val areaPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                            lineTo(points.last().x, sizeH)
                            lineTo(points.first().x, sizeH)
                            close()
                        }

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Gold.copy(alpha = 0.25f), Color.Transparent),
                                startY = 0f,
                                endY = sizeH
                            )
                        )
                    }

                    // 4. Draw the live EMA / SMA-9 line (neon blue color representation)
                    for (i in 0 until maPoints.size - 1) {
                        drawLine(
                            color = Color(0xFF2196F3),
                            start = maPoints[i],
                            end = maPoints[i + 1],
                            strokeWidth = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                    }

                    // 5. Draw the actual elegant price line (Gold theme line matching Recharts)
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = Gold,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 4f
                        )
                    }

                    // 6. Draw real-time scalping signals (Upward triangle for BUY, Downward for SELL)
                    for (i in scalpingSignals.indices) {
                        val signal = scalpingSignals[i] ?: continue
                        if (i in points.indices) {
                            val pt = points[i]
                            if (signal == "BUY") {
                                val triPath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(pt.x, pt.y - 12f)
                                    lineTo(pt.x - 7f, pt.y - 2f)
                                    lineTo(pt.x + 7f, pt.y - 2f)
                                    close()
                                }
                                drawPath(triPath, color = Color(0xFF00E676))
                            } else if (signal == "SELL") {
                                val triPath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(pt.x, pt.y + 12f)
                                    lineTo(pt.x - 7f, pt.y + 2f)
                                    lineTo(pt.x + 7f, pt.y + 2f)
                                    close()
                                }
                                drawPath(triPath, color = Color(0xFFFF5252))
                            }
                        }
                    }

                    // 7. Draw interactive hover crosshairs and target dot if user is dragging/tapping
                    hoverIndex?.let { idx ->
                        if (idx in points.indices) {
                            val targetPoint = points[idx]

                            // Vertical line (recharts tooltip marker)
                            drawLine(
                                color = Color.White.copy(alpha = 0.5f),
                                start = Offset(targetPoint.x, 0f),
                                end = Offset(targetPoint.x, sizeH),
                                strokeWidth = 1.6f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )

                            // Horizontal line crossing the Y value
                            drawLine(
                                color = Color.White.copy(alpha = 0.25f),
                                start = Offset(0f, targetPoint.y),
                                end = Offset(sizeW, targetPoint.y),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )

                            // Outer glowing highlight circle
                            drawCircle(
                                color = GoldLight.copy(alpha = 0.4f),
                                radius = 10.dp.toPx(),
                                center = targetPoint
                            )

                            // Inner clean target solid circle
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = targetPoint
                            )
                        }
                    }
                }

                // Inline floating Tooltip Card (recharts custom tooltip style) layered smoothly over canvas
                hoverIndex?.let { idx ->
                    if (idx in ticks.indices) {
                        val tick = ticks[idx]
                        val rsiVal = liveRSI.getOrNull(idx) ?: 50.0
                        val maVal = liveMA.getOrNull(idx) ?: tick.price
                        val signal = scalpingSignals.getOrNull(idx)
                        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(tick.timestamp))
                        
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xEC1A1530)),
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Scrubbed Tick Details",
                                        fontSize = 10.sp,
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (signal != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (signal == "BUY") Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFFFF5252).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "SCALP $signal",
                                                color = if (signal == "BUY") Color(0xFF00E676) else Color(0xFFFF5252),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Price: $${"%.4f".format(tick.price)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = GoldLight
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row {
                                    Text(
                                        text = "RSI(7): ${"%.1f".format(rsiVal)}  •  ",
                                        fontSize = 9.sp,
                                        color = Gold
                                    )
                                    Text(
                                        text = "MA(9): $${"%.4f".format(maVal)}",
                                        fontSize = 9.sp,
                                        color = Color(0xFF2196F3)
                                    )
                                }
                                Text(
                                    text = "Time: $timeStr",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Quick instruction/tip bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: $${"%.4f".format(yMin)}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = "⚡ Real-Time Scalping Signals Active ⚡",
                    fontSize = 10.sp,
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Max: $${"%.4f".format(yMax)}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
