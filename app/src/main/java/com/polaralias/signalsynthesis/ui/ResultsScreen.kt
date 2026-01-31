package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandPrimary)
                        }
                        
                        RainbowMcpText(
                            text = "INTELLIGENCE",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Black)
                        )

                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Transparent)
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            val fullResult = uiState.result
            if (fullResult == null) {
                Box(
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.WifiOff, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "NO SIGNAL STREAM", 
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        com.polaralias.signalsynthesis.ui.components.GlassBox(
                            modifier = Modifier
                                .height(44.dp)
                                .padding(horizontal = 32.dp)
                                .clickable { onBack() }
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("RECONNECT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary)
                            }
                        }
                    }
                }
                return@Scaffold
            }

            val filteredSetups = fullResult.setups.filter { !uiState.removedAlerts.contains(it.symbol) }

            Column(modifier = Modifier.padding(paddingValues)) {


                if (uiState.isPrefetching) {
                    com.polaralias.signalsynthesis.ui.components.GlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.5.dp, color = BrandPrimary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    "PROCESSING AI SUMMARY", 
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { uiState.prefetchCount.toFloat() / filteredSetups.size.coerceAtMost(3).toFloat() },
                                modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                                color = BrandPrimary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (fullResult.globalNotes.isNotEmpty()) {
                        item {
                            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandSecondary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("STRATEGY NOTES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandSecondary, letterSpacing = 1.sp)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    fullResult.globalNotes.forEach { note ->
                                        Text(
                                            text = "• $note",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
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
                    item { Spacer(modifier = Modifier.height(48.dp)) }
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
                verticalAlignment = Alignment.Top
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
                            color = BrandPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        SourceBadge(setup.source)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IntentBadge(setup.intent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            setup.setupType.uppercase(), 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Black,
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
                            color = if (setup.confidence > 0.7) SuccessGreen else BrandPrimary
                        )
                        Text("CONFIDENCE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp), fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = onToggleWatchlist, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Filled.Star else Icons.Filled.StarBorder, 
                            contentDescription = "Watchlist", 
                            tint = if (isInWatchlist) WarningOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PriceParam("TRIGGER", formatPrice(setup.triggerPrice), BrandPrimary)
                PriceParam("STOP LOSS", formatPrice(setup.stopLoss), ErrorRed)
                PriceParam("TARGET", formatPrice(setup.targetPrice), SuccessGreen)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                    .padding(16.dp)
            ) {
                val summary = when {
                    !hasLlmKey -> "Connect LLM for detailed analysis"
                    aiSummary?.status == AiSummaryStatus.READY -> aiSummary.summary.orEmpty()
                    aiSummary?.status == AiSummaryStatus.LOADING -> "Processing AI Summary..."
                    aiSummary?.status == AiSummaryStatus.ERROR -> "Analysis service unavailable"
                    else -> "Detailed analysis pending"
                }
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp).padding(top = 2.dp), 
                        tint = BrandSecondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        summary, 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    }
                    IconButton(onClick = onBlock, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Block, contentDescription = "Block", modifier = Modifier.size(18.dp), tint = ErrorRed.copy(alpha = 0.4f))
                    }
                }
                
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("VIEW DETAILS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun PriceParam(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = color)
    }
}

