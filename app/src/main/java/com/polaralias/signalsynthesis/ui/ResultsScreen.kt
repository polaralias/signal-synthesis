package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.ui.theme.*

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

    AmbientBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        RainbowMcpText(
                            text = "INTELLIGENCE REPORT", 
                            style = MaterialTheme.typography.titleLarge
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = RainbowBlue
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = RainbowBlue
                    ),
                    windowInsets = WindowInsets.systemBars
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
        val fullResult = uiState.result
        if (fullResult == null) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "NO DATA STREAM DETECTED", 
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onBack) {
                    Text("RETURN TO ENGINE", fontWeight = FontWeight.Bold)
                }
            }
            return@Scaffold
        }

        val filteredSetups = fullResult.setups.filter { !uiState.removedAlerts.contains(it.symbol) }

            Column(modifier = Modifier.padding(paddingValues)) {
                MockModeBanner(
                    isVisible = !uiState.hasAnyApiKeys
                )

                if (uiState.isPrefetching) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "Synthesizing AI Insights... [${uiState.prefetchCount}/${filteredSetups.size.coerceAtMost(3)} Nodes]",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpen)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            setup.symbol, 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = RainbowBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        SourceBadge(setup.source)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IntentBadge(setup.intent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            setup.setupType.uppercase(), 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatPercent(setup.confidence), 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (setup.confidence > 0.7) RainbowGreen else RainbowBlue
                        )
                        Text("CONFIDENCE", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = RainbowBlue.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = onToggleWatchlist, modifier = Modifier.size(32.dp)) {
                        Text(if (isInWatchlist) "⭐" else "☆", fontSize = 20.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PriceParam("TRIGGER", formatPrice(setup.triggerPrice), RainbowBlue)
                PriceParam("STOP LOSS", formatPrice(setup.stopLoss), RainbowRed)
                PriceParam("TARGET", formatPrice(setup.targetPrice), RainbowGreen)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(12.dp)
            ) {
                val summary = when {
                    !hasLlmKey -> "Connect LLM for AI Synthesis"
                    aiSummary?.status == AiSummaryStatus.READY -> aiSummary.summary.orEmpty()
                    aiSummary?.status == AiSummaryStatus.LOADING -> "Synthesizing AI Insights..."
                    aiSummary?.status == AiSummaryStatus.ERROR -> "AI Node Offline"
                    else -> "Analyze deeper for AI synthesis"
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text("🤖", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        summary, 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpen) {
                    Text("VIEW FULL SPECTRUM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun PriceParam(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

