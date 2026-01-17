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
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

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
            Text("Provide your data provider keys. Keys are stored locally.")
            OutlinedTextField(
                value = uiState.keys.alpacaKey,
                onValueChange = { onFieldChanged(KeyField.ALPACA_KEY, it) },
                label = { Text("Alpaca API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.keys.alpacaSecret,
                onValueChange = { onFieldChanged(KeyField.ALPACA_SECRET, it) },
                label = { Text("Alpaca Secret") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.keys.polygonKey,
                onValueChange = { onFieldChanged(KeyField.POLYGON, it) },
                label = { Text("Polygon API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.keys.finnhubKey,
                onValueChange = { onFieldChanged(KeyField.FINNHUB, it) },
                label = { Text("Finnhub API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.keys.fmpKey,
                onValueChange = { onFieldChanged(KeyField.FMP, it) },
                label = { Text("FMP API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            SectionHeader("AI Key")
            OutlinedTextField(
                value = uiState.keys.llmKey,
                onValueChange = { onFieldChanged(KeyField.LLM, it) },
                label = { Text("LLM API Key") },
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
