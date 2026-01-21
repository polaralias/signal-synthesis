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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.polaralias.signalsynthesis.domain.ai.LlmProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onFieldChanged: (KeyField, String) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Keys") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.blacklistedProviders.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "⚠️ Provider Blocked",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "One or more providers returned a 403 Forbidden error and are temporarily paused (10min). Please check your API keys.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text("Provide your data provider keys. Keys are stored locally.")
            
            ApiKeyField(
                value = uiState.keys.alpacaKey,
                onValueChange = { onFieldChanged(KeyField.ALPACA_KEY, it) },
                label = "Alpaca API Key",
                isBlacklisted = uiState.blacklistedProviders.contains("AlpacaMarketDataProvider")
            )
            
            OutlinedTextField(
                value = uiState.keys.alpacaSecret,
                onValueChange = { onFieldChanged(KeyField.ALPACA_SECRET, it) },
                label = { Text("Alpaca Secret") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            ApiKeyField(
                value = uiState.keys.polygonKey,
                onValueChange = { onFieldChanged(KeyField.POLYGON, it) },
                label = "Polygon API Key",
                isBlacklisted = uiState.blacklistedProviders.contains("PolygonMarketDataProvider")
            )

            ApiKeyField(
                value = uiState.keys.finnhubKey,
                onValueChange = { onFieldChanged(KeyField.FINNHUB, it) },
                label = "Finnhub API Key",
                isBlacklisted = uiState.blacklistedProviders.contains("FinnhubMarketDataProvider")
            )

            ApiKeyField(
                value = uiState.keys.fmpKey,
                onValueChange = { onFieldChanged(KeyField.FMP, it) },
                label = "FMP API Key",
                isBlacklisted = uiState.blacklistedProviders.contains("FmpMarketDataProvider")
            )

            ApiKeyField(
                value = uiState.keys.twelveDataKey,
                onValueChange = { onFieldChanged(KeyField.TWELVE_DATA, it) },
                label = "Twelve Data API Key",
                isBlacklisted = uiState.blacklistedProviders.contains("TwelveDataMarketDataProvider")
            )

            SectionHeader("AI Key")
            val llmLabel = when (uiState.appSettings.llmProvider) {
                LlmProvider.OPENAI -> "OpenAI API Key"
                LlmProvider.GEMINI -> "Gemini API Key"
            }
            OutlinedTextField(
                value = uiState.keys.llmKey,
                onValueChange = { onFieldChanged(KeyField.LLM, it) },
                label = { Text(llmLabel) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = onSave) {
                    Text("Save Keys")
                }
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isBlacklisted: Boolean
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isBlacklisted,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = if (isBlacklisted) {
                { Text("🚫", modifier = Modifier.padding(end = 8.dp)) }
            } else null
        )
        if (isBlacklisted) {
            Text(
                "Provider temporarily paused due to authentication errors.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
