package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.polaralias.signalsynthesis.domain.model.IndexQuote
import com.polaralias.signalsynthesis.domain.model.TradingIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: AnalysisUiState,
    onIntentSelected: (TradingIntent) -> Unit,
    onRefreshMarket: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenResults: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signal Synthesis", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onRefreshMarket) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Market")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Market Overview Section
            MarketSection(
                indices = uiState.marketOverview?.indices ?: emptyList(),
                lastUpdated = uiState.marketOverview?.lastUpdated,
                isLoading = uiState.isLoadingMarket
            )

            // Quick Analysis Section
            QuickAnalysisSection(onIntentSelected = onIntentSelected)

            // Recent Results Section
            if (uiState.result != null) {
                RecentResultsSection(
                    uiState = uiState,
                    onOpenResults = onOpenResults,
                    onOpenDetail = onOpenDetail
                )
            }

            // Alerts Status
            AlertStatusCard(
                enabled = uiState.alertsEnabled,
                count = uiState.alertSymbolCount,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@Composable
private fun MarketSection(
    indices: List<IndexQuote>,
    lastUpdated: java.time.Instant?,
    isLoading: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Market Overview", style = MaterialTheme.typography.titleMedium)
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = "Updated: ${formatTime(lastUpdated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (indices.isEmpty() && !isLoading) {
            Text("Market data unavailable.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(indices) { index ->
                    IndexCard(index)
                }
            }
        }
    }
}

@Composable
private fun IndexCard(index: IndexQuote) {
    val color = if (index.changePercent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(index.symbol, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(index.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatPrice(index.price), fontWeight = FontWeight.Bold)
            Text(
                text = "${if (index.changePercent >= 0) "+" else ""}${String.format("%.2f", index.changePercent)}%",
                color = color,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun QuickAnalysisSection(onIntentSelected: (TradingIntent) -> Unit) {
    Column {
        Text("Run Analysis", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalysisButton("Day", Modifier.weight(1f)) { onIntentSelected(TradingIntent.DAY_TRADE) }
            AnalysisButton("Swing", Modifier.weight(1f)) { onIntentSelected(TradingIntent.SWING) }
            AnalysisButton("Long", Modifier.weight(1f)) { onIntentSelected(TradingIntent.LONG_TERM) }
        }
    }
}

@Composable
private fun AnalysisButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(label)
    }
}

@Composable
private fun RecentResultsSection(
    uiState: AnalysisUiState,
    onOpenResults: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val setups = uiState.result?.setups?.take(3) ?: emptyList()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Findings", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onOpenResults) {
                Text("View All")
            }
        }
        if (setups.isEmpty()) {
            Text("No setups found in last run.", style = MaterialTheme.typography.bodyMedium)
        } else {
            setups.forEach { setup ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onOpenDetail(setup.symbol) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(setup.symbol, fontWeight = FontWeight.Bold)
                                if (setup.isUserAdded) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("👤", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IntentBadge(setup.intent)
                            }
                            Text(setup.setupType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(formatPercent(setup.confidence), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertStatusCard(enabled: Boolean, count: Int, onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSettings() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (enabled) "🔔" else "🔕", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (enabled) "Market Alerts Active" else "Market Alerts Paused",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (enabled) "Monitoring $count symbols" else "Enable in settings to stay updated",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
