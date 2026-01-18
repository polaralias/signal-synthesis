package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polaralias.signalsynthesis.data.settings.AppSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onEditKeys: () -> Unit,
    onClearKeys: () -> Unit,
    onToggleAlerts: (Boolean) -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onSuggestAi: (String) -> Unit,
    onApplyAi: () -> Unit,
    onDismissAi: () -> Unit
) {
    var showAiDialog by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SectionHeader("Key Status")
            val providerStatus = if (uiState.hasAnyApiKeys) "Configured" else "Missing"
            val llmStatus = if (uiState.hasLlmKey) "Configured" else "Missing"
            Text("Provider keys: $providerStatus")
            Text("LLM key: $llmStatus")

            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Button(onClick = onEditKeys) {
                    Text("Edit Keys")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                OutlinedButton(onClick = onClearKeys) {
                    Text("Clear Keys")
                }
            }

            SectionHeader("Alerts")
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Enable background alerts",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = uiState.alertsEnabled,
                    onCheckedChange = onToggleAlerts
                )
            }
            Text(
                text = "Monitoring ${uiState.alertSymbolCount} symbols from your last analysis run.",
                style = MaterialTheme.typography.bodyMedium
            )

            SectionHeader("Alert Thresholds")
            
            SettingsSlider(
                label = "VWAP Dip %",
                value = uiState.appSettings.vwapDipPercent,
                range = 0.1f..5.0f,
                steps = 49,
                format = { String.format("%.1f%%", it) },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(vwapDipPercent = it)) }
            )

            SettingsSlider(
                label = "RSI Oversold",
                value = uiState.appSettings.rsiOversold,
                range = 10.0f..50.0f,
                steps = 40,
                format = { it.roundToInt().toString() },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(rsiOversold = it)) }
            )

            SettingsSlider(
                label = "RSI Overbought",
                value = uiState.appSettings.rsiOverbought,
                range = 50.0f..90.0f,
                steps = 40,
                format = { it.roundToInt().toString() },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(rsiOverbought = it)) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            if (uiState.aiThresholdSuggestion != null) {
                AiSuggestionCard(
                    suggestion = uiState.aiThresholdSuggestion,
                    onApply = onApplyAi,
                    onDismiss = onDismissAi
                )
            } else {
                Button(
                    onClick = { showAiDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.hasLlmKey && !uiState.isSuggestingThresholds
                ) {
                    Text(if (uiState.isSuggestingThresholds) "Analyzing..." else "Ask AI for suggestions")
                }
            }

            SectionHeader("App")
            Text(
                text = "AI synthesis runs when an LLM key is configured. Alerts run every 15 minutes when enabled.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("Ask AI for Thresholds") },
            text = {
                Column {
                    Text("Describe your trading style or risk tolerance (e.g., 'I am a conservative swing trader' or 'I have $5000 to invest').")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter context...") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSuggestAi(aiPrompt)
                    showAiDialog = false
                }) {
                    Text("Suggest")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAiDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Double,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Double) -> String,
    onValueChange: (Double) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = label, modifier = Modifier.weight(1f))
            Text(text = format(value))
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = range,
            steps = steps
        )
    }
}

@Composable
private fun AiSuggestionCard(
    suggestion: com.polaralias.signalsynthesis.ui.AiThresholdSuggestion,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AI Suggestions",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "VWAP Dip: ${String.format("%.1f%%", suggestion.vwapDipPercent)}")
            Text(text = "RSI Oversold: ${suggestion.rsiOversold.roundToInt()}")
            Text(text = "RSI Overbought: ${suggestion.rsiOverbought.roundToInt()}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = suggestion.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Button(onClick = onApply) {
                    Text("Apply")
                }
                Spacer(modifier = Modifier.padding(4.dp))
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}
