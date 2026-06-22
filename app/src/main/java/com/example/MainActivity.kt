package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.*
import com.example.ui.theme.Gold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TradeAIApplication
        val factory = ViewModelFactory(app)

        // Standard Jetpack ViewModel Providers representing MVVM manual DI
        val marketViewModel = ViewModelProvider(this, factory)[MarketViewModel::class.java]
        val signalViewModel = ViewModelProvider(this, factory)[SignalViewModel::class.java]
        val chatViewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]
        val settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf("Home") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val items = listOf("Home", "Watchlist", "Signals", "Chart", "Chat", "News", "Settings")
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            items.forEach { tab ->
                                val selected = currentTab == tab
                                val icon = when (tab) {
                                    "Home" -> Icons.Default.Home
                                    "Watchlist" -> Icons.Default.Star
                                    "Signals" -> Icons.Default.Bolt
                                    "Chart" -> Icons.Default.ShowChart
                                    "Chat" -> Icons.Default.ChatBubble
                                    "News" -> Icons.Default.Newspaper
                                    else -> Icons.Default.Settings
                                }

                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    label = { Text(text = tab, color = if (selected) Gold else Color.Gray) },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = tab,
                                            tint = if (selected) Gold else Color.Gray,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Gold.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_item_$tab")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            "Home" -> HomeScreen(
                                marketViewModel = marketViewModel,
                                signalViewModel = signalViewModel,
                                settingsViewModel = settingsViewModel,
                                onNavigateToTab = { currentTab = it }
                            )
                            "Watchlist" -> WatchlistScreen(
                                viewModel = marketViewModel,
                                onNavigateToTab = { currentTab = it }
                            )
                            "Signals" -> SignalsScreen(
                                viewModel = signalViewModel
                            )
                            "Chart" -> ChartScreen(
                                viewModel = marketViewModel
                            )
                            "Chat" -> ChatScreen(
                                viewModel = chatViewModel
                            )
                            "News" -> NewsScreen(
                                viewModel = settingsViewModel
                            )
                            "Settings" -> SettingsScreen(
                                viewModel = settingsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
