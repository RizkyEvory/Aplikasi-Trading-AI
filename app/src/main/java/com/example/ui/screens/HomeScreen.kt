package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.NewsArticle
import com.example.data.model.ScalpSignal
import com.example.data.model.WatchlistItem
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MutedText
import com.example.ui.viewmodel.MarketViewModel
import com.example.ui.viewmodel.SignalViewModel
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    marketViewModel: MarketViewModel,
    signalViewModel: SignalViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToTab: (String) -> Unit // Navigates to custom tab
) {
    val watchlist by marketViewModel.watchlist.collectAsState()
    val signals by signalViewModel.scalpSignals.collectAsState()
    val articles by settingsViewModel.newsArticles.collectAsState()

    val totalSignals = signals.size
    val openSignals = signals.filter { it.status == "OPEN" }
    val winRatio = if (signals.filter { it.status != "OPEN" }.isEmpty()) 80.0 else {
        val totalHist = signals.filter { it.status != "OPEN" }
        (totalHist.count { it.status == "WIN" }.toDouble() / totalHist.size) * 100.0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Brand Header Hero banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Gold.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "M4DI",
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold,
                        fontSize = 32.sp,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "TradeAI Pro",
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Universal Intelligence. Scalp Efficiency.",
                        fontSize = 11.sp,
                        color = MutedText
                    )
                }
            }
        }

        // Performance review highlights widgets
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatsCardItem(
                    title = "Win Rate",
                    value = "%.1f%%".format(winRatio),
                    sub = "Based on history",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF4CAF50)
                )
                StatsCardItem(
                    title = "Pending Setups",
                    value = "${openSignals.size} Active",
                    sub = "Scalp signals",
                    modifier = Modifier.weight(1f),
                    color = Gold
                )
            }
        }

        // Live market tickers horizontal scroller
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "PINNED MARKETS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )

                if (watchlist.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { onNavigateToTab("Watchlist") },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Watchlist is currently empty.", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Tap here to search and Pin assets (Crypto, Stocks, Forex) to get real-time price tracker feeds.", fontSize = 11.sp, color = MutedText)
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(watchlist) { item ->
                            TickerItem(item = item, onClick = {
                                marketViewModel.selectSymbol(item.symbol, item.name)
                                onNavigateToTab("Chart")
                            })
                        }
                    }
                }
            }
        }

        // Signal Highlight Widget
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UPCOMING SIGNAL SCALPS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                    TextButton(onClick = { onNavigateToTab("Signals") }) {
                        Text(text = "View All", color = GoldLight, fontSize = 12.sp)
                    }
                }

                val recentList = signals.take(2)
                if (recentList.isEmpty()) {
                    Text(text = "No indicators cross notifications yet.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (sig in recentList) {
                            BriefSignalRow(sig = sig, onClick = {
                                marketViewModel.selectSymbol(sig.symbol, sig.name)
                                onNavigateToTab("Chart")
                            })
                        }
                    }
                }
            }
        }

        // Sentiment news feed
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "INTELLIGENCE HEADLINES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val headCount = articles.take(3)
                if (headCount.isEmpty()) {
                    Text(text = "Sentiment analytics feed is initializing...", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (art in headCount) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = art.title ?: "",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = art.description ?: "",
                                        fontSize = 11.sp,
                                        color = MutedText,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Source: ${art.source?.name ?: "News"}", fontSize = 9.sp, color = GoldLight)
                                        Text(text = "Sentiment: Bullish", fontSize = 9.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCardItem(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 11.sp, color = MutedText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = sub, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TickerItem(item: WatchlistItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .height(105.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = item.symbol, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Text(text = item.type, fontSize = 9.sp, color = MutedText)

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$%.2f".format(item.price),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = GoldLight
                )

                val isUp = item.change >= 0
                val col = if (isUp) Color(0xFF4CAF50) else Color(0xFFF44336)
                Text(
                    text = "%s%.2f%%".format(if (isUp) "+" else "", item.change),
                    fontSize = 10.sp,
                    color = col,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BriefSignalRow(sig: ScalpSignal, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val signValColor = if (sig.direction == "BUY") Color(0xFF4CAF50) else Color(0xFFF44336)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(signValColor, RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = sig.symbol, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = "Timeframe: ${sig.timeframe} | Conf: ${sig.confidence.toInt()}%", fontSize = 10.sp, color = MutedText)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Target: ${sig.entryPrice}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldLight)
                Text(text = sig.direction, fontSize = 10.sp, color = if (sig.direction == "BUY") Color(0xFF4CAF50) else Color(0xFFF44336), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
