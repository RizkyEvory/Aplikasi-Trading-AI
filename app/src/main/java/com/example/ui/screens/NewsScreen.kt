package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.NewsArticle
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MutedText
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(viewModel: SettingsViewModel) {
    val articles by viewModel.newsArticles.collectAsState()
    val isLoading by viewModel.isLoadingNews.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SENTIMENT HEADLINES",
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        fontSize = 16.sp
                    )
                }
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Gold
                )
            } else if (articles.isEmpty()) {
                Text(
                    text = "No intelligence news articles parsed.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(articles) { article ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!article.url.isNullOrBlank()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                                        context.startActivity(intent)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = article.title ?: "Untitled Headline",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = article.description ?: "No description available.",
                                    fontSize = 11.sp,
                                    color = MutedText,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "By ${article.author ?: "TradeAI Analysts"}",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Source: ${article.source?.name ?: "Market Feed"}",
                                            fontSize = 9.sp,
                                            color = GoldLight
                                        )
                                    }

                                    // Auto computed AI sentiment analysis (Bullish/Bearish tag)
                                    val isBullish = article.title?.lowercase()?.contains("bull") == true ||
                                                article.title?.lowercase()?.contains("rise") == true ||
                                                article.title?.lowercase()?.contains("reclaim") == true ||
                                                article.title?.lowercase()?.contains("success") == true ||
                                                article.title?.lowercase()?.contains("growth") == true ||
                                                article.title?.lowercase()?.contains("positive") == true

                                    val tag = if (isBullish) "BULLISH" else "NEUTRAL"
                                    val tagColor = if (isBullish) Color(0xFF4CAF50) else Color(0xFFFF8F00)

                                    Badge(containerColor = tagColor.copy(alpha = 0.15f), contentColor = tagColor) {
                                        Text(
                                            text = tag,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
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
