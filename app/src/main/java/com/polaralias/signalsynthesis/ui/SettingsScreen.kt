package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onEditKeys: () -> Unit,
    onClearKeys: () -> Unit,
    onToggleAlerts: (Boolean) -> Unit
) {
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

            SectionHeader("App")
            Text(
                text = "AI synthesis runs when an LLM key is configured. Alerts run every 15 minutes when enabled.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
