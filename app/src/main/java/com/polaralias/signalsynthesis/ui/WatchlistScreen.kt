package com.polaralias.signalsynthesis.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onOpenSymbol: (String) -> Unit,
    onRemove: (String) -> Unit
) {
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
                        isUserAdded = setup?.isUserAdded ?: false,
                        onClick = { onOpenSymbol(symbol) },
                        onRemove = { onRemove(symbol) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchlistItem(
    symbol: String,
    intent: com.polaralias.signalsynthesis.domain.model.TradingIntent?,
    isUserAdded: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(symbol, style = MaterialTheme.typography.titleMedium)
                if (isUserAdded) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                    Text("👤", style = MaterialTheme.typography.labelSmall)
                }
                intent?.let {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    IntentBadge(it)
                }
            }
            IconButton(onClick = onRemove) {
                Text("Remove")
            }
        }
    }
}
