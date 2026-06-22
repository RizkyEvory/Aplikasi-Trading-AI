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
import com.example.engine.SignalEngine
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.viewmodel.MarketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(viewModel: MarketViewModel) {
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val selectedName by viewModel.selectedAssetName.collectAsState()
    val candles by viewModel.chartCandles.collectAsState()
    val isLoading by viewModel.isLoadingChart.collectAsState()
    val selectedInterval by viewModel.selectedInterval.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()

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
            // Timeframes selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold)
                }
            } else if (candles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No Chart Data Available. Check API logs.", color = Color.Gray)
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
