package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MutedText
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val savedKeys by viewModel.savedKeys.collectAsState()
    val priceAlerts by viewModel.priceAlerts.collectAsState()
    val statusMsg by viewModel.alertStatusMsg.collectAsState()

    // Key inputs
    val twKey by viewModel.twelveDataKeyInput.collectAsState()
    val fhKey by viewModel.finnhubKeyInput.collectAsState()
    val newsKey by viewModel.newsApiKeyInput.collectAsState()
    val orKey by viewModel.openRouterKeyInput.collectAsState()

    // Alert inputs
    var alertSymbol by remember { mutableStateOf("") }
    var alertPrice by remember { mutableStateOf("") }
    var alertCondition by remember { mutableStateOf("ABOVE") } // ABOVE, BELOW

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Multi API Key Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MANAGE MULTI API KEYS",
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter your API integration keys below. Credentials will rotate automatically when limits are matched.",
                        fontSize = 10.sp,
                        color = MutedText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    KeyInputRow(
                        label = "Twelve Data API Key (Charts)",
                        value = twKey,
                        onValueChange = { viewModel.twelveDataKeyInput.value = it },
                        tag = "twelve_data_key"
                    )

                    KeyInputRow(
                        label = "Finnhub API Key (Company profiles)",
                        value = fhKey,
                        onValueChange = { viewModel.finnhubKeyInput.value = it },
                        tag = "finnhub_key"
                    )

                    KeyInputRow(
                        label = "NewsAPI Key (Intelligence headlines)",
                        value = newsKey,
                        onValueChange = { viewModel.newsApiKeyInput.value = it },
                        tag = "news_api_key"
                    )

                    KeyInputRow(
                        label = "OpenRouter API Key (AI Chatbot Advisor)",
                        value = orKey,
                        onValueChange = { viewModel.openRouterKeyInput.value = it },
                        tag = "open_router_key"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveAllKeys()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("save_keys_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Save Credentials", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.resetKeyLimits() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_rotation_button")
                        ) {
                            Text(text = "Reset Limits", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Custom price alerts rule maker
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CREATE CUSTOM PRICE ALERTS",
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trigger local notification pushes when asset price matches target specifications.",
                        fontSize = 10.sp,
                        color = MutedText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = alertSymbol,
                        onValueChange = { alertSymbol = it },
                        placeholder = { Text("Symbol, e.g. BTC/USD") },
                        label = { Text("Asset Ticker") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alert_symbol_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            focusedLabelColor = Gold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = alertPrice,
                        onValueChange = { alertPrice = it },
                        placeholder = { Text("Price target, e.g 68500.00") },
                        label = { Text("Target price") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alert_price_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            focusedLabelColor = Gold
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Trigger Condition:", fontSize = 12.sp, color = Color.White)

                        Row {
                            ConditionSelectButton(
                                text = "ABOVE",
                                isSelected = alertCondition == "ABOVE",
                                onClick = { alertCondition = "ABOVE" }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            ConditionSelectButton(
                                text = "BELOW",
                                isSelected = alertCondition == "BELOW",
                                onClick = { alertCondition = "BELOW" }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val priceD = alertPrice.toDoubleOrNull() ?: 0.0
                            viewModel.createPriceAlert(alertSymbol, priceD, alertCondition)
                            alertSymbol = ""
                            alertPrice = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_alert_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Save Alert Action", fontWeight = FontWeight.Bold)
                    }

                    if (statusMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = statusMsg, color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Alerts List Table
        item {
            Text(
                text = "ACTIVE PRICE ACTION ALERTS",
                fontWeight = FontWeight.Bold,
                color = Gold,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (priceAlerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No custom price alerts configured.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(priceAlerts) { alert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_row_${alert.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (alert.isActive) Color(0xFF4CAF50) else Color.Gray,
                                        RoundedCornerShape(50)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = alert.symbol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Crosses ${alert.condition.lowercase()} target $${alert.targetPrice}",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = alert.isActive,
                                onCheckedChange = { viewModel.togglePriceAlertActive(alert) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Gold,
                                    checkedTrackColor = Gold.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = { viewModel.deletePriceAlert(alert) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Alert", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(text = label, fontSize = 11.sp, color = GoldLight)
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("api_********************") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                focusedLabelColor = Gold
            )
        )
    }
}

@Composable
fun ConditionSelectButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) Gold else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Gold else Color.Gray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}
