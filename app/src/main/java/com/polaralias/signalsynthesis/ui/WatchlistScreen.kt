package com.polaralias.signalsynthesis.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onOpenSymbol: (String) -> Unit,
    onRemove: (String) -> Unit,
    onBlock: (String) -> Unit
) {
    var symbolToBlock by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watchlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.watchlist.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text("Your watchlist is empty.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.watchlist) { symbol ->
                    val setup = uiState.result?.setups?.find { it.symbol == symbol }
                        ?: uiState.history.flatMap { it.setups }.find { it.symbol == symbol }
                    
                    WatchlistItem(
                        symbol = symbol,
                        intent = setup?.intent,
                        source = setup?.source ?: com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED,
                        onClick = { onOpenSymbol(symbol) },
                        onRemove = { onRemove(symbol) },
                        onBlock = { symbolToBlock = symbol }
                    )
                }
            }
        }

        if (symbolToBlock != null) {
            ConfirmBlocklistDialog(
                symbol = symbolToBlock!!,
                onConfirm = {
                    onBlock(symbolToBlock!!)
                    symbolToBlock = null
                },
                onDismiss = { symbolToBlock = null }
            )
        }
    }
}

@Composable
private fun WatchlistItem(
    symbol: String,
    intent: com.polaralias.signalsynthesis.domain.model.TradingIntent?,
    source: com.polaralias.signalsynthesis.domain.model.TickerSource,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(symbol, style = MaterialTheme.typography.titleMedium)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                SourceBadge(source)
                intent?.let {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                    IntentBadge(it)
                }
            }
            IconButton(onClick = onRemove) {
                Text("Remove")
            }
            IconButton(onClick = onBlock) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Block",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
