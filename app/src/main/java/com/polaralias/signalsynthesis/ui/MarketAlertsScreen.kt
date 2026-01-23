package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketAlertsScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onRemoveAlert: (String) -> Unit,
    onAddToBlocklist: (String) -> Unit
) {
    var symbolToBlock by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market Alerts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.alertSymbols.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No active alerts. New signals will appear here.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.alertSymbols) { symbol ->
                    AlertItem(
                        symbol = symbol,
                        onOpen = { onOpenDetail(symbol) },
                        onRemove = { onRemoveAlert(symbol) },
                        onBlock = { symbolToBlock = symbol }
                    )
                }
            }
        }

        if (symbolToBlock != null) {
            ConfirmBlocklistDialog(
                symbol = symbolToBlock!!,
                onConfirm = {
                    onAddToBlocklist(symbolToBlock!!)
                    symbolToBlock = null
                },
                onDismiss = { symbolToBlock = null }
            )
        }
    }
}

@Composable
private fun AlertItem(
    symbol: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Currently monitoring for signals", style = MaterialTheme.typography.bodySmall)
            }
            
            IconButton(onClick = onOpen) {
                Icon(Icons.Default.Info, contentDescription = "Analysis Info", tint = MaterialTheme.colorScheme.primary)
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Exclude Temporarily", tint = MaterialTheme.colorScheme.outline)
            }
            
            IconButton(onClick = onBlock) {
                Icon(Icons.Default.Block, contentDescription = "Add to Blocklist", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ConfirmBlocklistDialog(
    symbol: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Blocklist $symbol?") },
        text = { Text("Are you sure you want to blocklist $symbol? It will be excluded from all future analysis and alerts until you remove it from the blocklist in settings.") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Blocklist")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
