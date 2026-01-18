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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polaralias.signalsynthesis.domain.model.TradingIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    uiState: AnalysisUiState,
    onIntentSelected: (TradingIntent) -> Unit,
    onRunAnalysis: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenResults: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWatchlist: () -> Unit,
    onOpenHistory: () -> Unit,
    onDismissError: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signal Synthesis") },
                actions = {
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

            SectionHeader("Keys")
            if (!uiState.hasAnyApiKeys) {
                Text(
                    text = "Add provider keys before running analysis.",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenKeys) {
                    Text("Configure Keys")
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
            Button(
                onClick = onRunAnalysis,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isLoading) "Running..." else "Run Analysis")
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
