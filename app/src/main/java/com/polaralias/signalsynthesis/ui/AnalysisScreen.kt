package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.polaralias.signalsynthesis.domain.model.TradingIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    uiState: AnalysisUiState,
    onIntentSelected: (TradingIntent) -> Unit,
    onAssetClassSelected: (com.polaralias.signalsynthesis.data.settings.AssetClass) -> Unit,
    onDiscoveryModeSelected: (com.polaralias.signalsynthesis.data.settings.DiscoveryMode) -> Unit,
    onRunAnalysis: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenResults: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWatchlist: () -> Unit,
    onOpenHistory: () -> Unit,
    onDismissError: () -> Unit,
    onClearNavigation: () -> Unit,
    onCancelAnalysis: () -> Unit,
    onTogglePause: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(uiState.navigationEvent) {
        if (uiState.navigationEvent is NavigationEvent.Results) {
            onOpenResults()
            onClearNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signal Synthesis") },
                actions = {
                    val pauseLabel = if (uiState.isPaused) "▶️ Resume" else "⏸️ Pause"
                    TextButton(onClick = onTogglePause) {
                        Text(pauseLabel)
                    }
                    IconButton(onClick = onOpenWatchlist) {
                        Text("⭐")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Text("🕒")
                    }
                    OutlinedButton(onClick = onOpenSettings) {
                        Text("Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.isPaused) {
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("⏸️", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Analysis Paused", style = MaterialTheme.typography.titleSmall)
                            Text("Background alerts and scheduled tasks are suspended.", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = onTogglePause) {
                            Text("Resume")
                        }
                    }
                }
            }

            SectionHeader("Asset Class")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetChip(
                    label = "Stocks",
                    selected = uiState.assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.STOCKS,
                    onClick = { onAssetClassSelected(com.polaralias.signalsynthesis.data.settings.AssetClass.STOCKS) }
                )
                AssetChip(
                    label = "Forex",
                    selected = uiState.assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.FOREX,
                    onClick = { onAssetClassSelected(com.polaralias.signalsynthesis.data.settings.AssetClass.FOREX) }
                )
                AssetChip(
                    label = "Metals",
                    selected = uiState.assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.METALS,
                    onClick = { onAssetClassSelected(com.polaralias.signalsynthesis.data.settings.AssetClass.METALS) }
                )
                AssetChip(
                    label = "All",
                    selected = uiState.assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.ALL,
                    onClick = { onAssetClassSelected(com.polaralias.signalsynthesis.data.settings.AssetClass.ALL) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("Discovery Mode")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetChip(
                    label = "Curated",
                    selected = uiState.appSettings.discoveryMode == com.polaralias.signalsynthesis.data.settings.DiscoveryMode.CURATED,
                    onClick = { onDiscoveryModeSelected(com.polaralias.signalsynthesis.data.settings.DiscoveryMode.CURATED) }
                )
                AssetChip(
                    label = "Live Scanner",
                    selected = uiState.appSettings.discoveryMode == com.polaralias.signalsynthesis.data.settings.DiscoveryMode.LIVE_SCANNER,
                    onClick = { onDiscoveryModeSelected(com.polaralias.signalsynthesis.data.settings.DiscoveryMode.LIVE_SCANNER) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("Intent")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntentChip(
                    label = "Day Trade",
                    selected = uiState.intent == TradingIntent.DAY_TRADE,
                    onClick = { onIntentSelected(TradingIntent.DAY_TRADE) }
                )
                IntentChip(
                    label = "Swing",
                    selected = uiState.intent == TradingIntent.SWING,
                    onClick = { onIntentSelected(TradingIntent.SWING) }
                )
                IntentChip(
                    label = "Long Term",
                    selected = uiState.intent == TradingIntent.LONG_TERM,
                    onClick = { onIntentSelected(TradingIntent.LONG_TERM) }
                )
            }

            if (uiState.blacklistedProviders.isNotEmpty()) {
                androidx.compose.material3.Surface(
                    onClick = onOpenKeys,
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("🚫")
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Providers Paused",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            "One or more market data providers are temporarily paused due to authentication errors. Tap to check your API keys.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            SectionHeader("Keys")
            if (!uiState.hasAnyApiKeys) {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MOCK MODE ACTIVE",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "No API keys configured. Using simulated data for demonstration.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenKeys) {
                    Text(if (uiState.hasAnyApiKeys) "Configure Keys" else "Add Keys")
                }
                OutlinedButton(onClick = onOpenSettings) {
                    Text("Settings")
                }
            }

            SectionHeader("Run Analysis")
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onDismissError) {
                    Text("Dismiss")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRunAnalysis,
                    enabled = !uiState.isLoading && !uiState.isPaused,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.isLoading) "Running..." else "Run Analysis")
                }
                
                if (uiState.isLoading) {
                    OutlinedButton(
                        onClick = onCancelAnalysis,
                        modifier = Modifier.weight(0.4f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Stop")
                    }
                }
            }

            SectionHeader("Summary")
            if (uiState.result == null) {
                Text("No analysis run yet.")
            } else {
                val result = uiState.result
                Text("Candidates: ${result.totalCandidates}")
                Text("Tradeable: ${result.tradeableCount}")
                Text("Setups: ${result.setupCount}")
                Text("Last run: ${formatTime(uiState.lastRunAt)}")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onOpenResults) {
                    Text("View Results")
                }
            }
        }
    }
}

@Composable
private fun IntentChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun AssetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
