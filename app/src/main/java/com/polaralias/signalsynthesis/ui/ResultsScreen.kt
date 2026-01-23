package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.polaralias.signalsynthesis.domain.model.TradeSetup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onRemoveTicker: (String) -> Unit,
    onBlockTicker: (String) -> Unit
) {
    var symbolToBlock by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        val fullResult = uiState.result
        if (fullResult == null) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text("Run analysis to see results.")
            }
            return@Scaffold
        }

        val filteredSetups = fullResult.setups.filter { !uiState.removedAlerts.contains(it.symbol) }

        Column(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isPrefetching) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "Generating AI insights... (${uiState.prefetchCount}/${filteredSetups.size.coerceAtMost(3)})",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSetups) { setup ->
                    SetupCard(
                        setup = setup,
                        hasLlmKey = uiState.hasLlmKey,
                        aiSummary = uiState.aiSummaries[setup.symbol],
                        isInWatchlist = uiState.watchlist.contains(setup.symbol),
                        onToggleWatchlist = { onToggleWatchlist(setup.symbol) },
                        onOpen = { onOpenDetail(setup.symbol) },
                        onRemove = { onRemoveTicker(setup.symbol) },
                        onBlock = { symbolToBlock = setup.symbol }
                    )
                }
            }
        }

        if (symbolToBlock != null) {
            ConfirmBlocklistDialog(
                symbol = symbolToBlock!!,
                onConfirm = {
                    onBlockTicker(symbolToBlock!!)
                    symbolToBlock = null
                },
                onDismiss = { symbolToBlock = null }
            )
        }
    }
}

@Composable
private fun SetupCard(
    setup: TradeSetup,
    hasLlmKey: Boolean,
    aiSummary: AiSummaryState?,
    isInWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpen),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(setup.symbol, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    SourceBadge(setup.source)
                    Spacer(modifier = Modifier.width(8.dp))
                    IntentBadge(setup.intent)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatPercent(setup.confidence), fontWeight = FontWeight.Bold)
                    IconButton(onClick = onToggleWatchlist) {
                        Text(if (isInWatchlist) "⭐" else "☆")
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                    IconButton(onClick = onBlock) {
                        Icon(Icons.Default.Block, contentDescription = "Block", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(setup.setupType, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Trigger ${formatPrice(setup.triggerPrice)}")
            Text("Stop ${formatPrice(setup.stopLoss)}")
            Text("Target ${formatPrice(setup.targetPrice)}")
            Spacer(modifier = Modifier.height(8.dp))
            val summary = when {
                !hasLlmKey -> "Add an LLM key to enable AI synthesis."
                aiSummary?.status == AiSummaryStatus.READY -> aiSummary.summary.orEmpty()
                aiSummary?.status == AiSummaryStatus.LOADING -> "Generating AI synthesis..."
                aiSummary?.status == AiSummaryStatus.ERROR -> "AI synthesis unavailable."
                else -> "Open to generate AI synthesis."
            }
            Text(summary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

