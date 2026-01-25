package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polaralias.signalsynthesis.domain.model.*
import com.polaralias.signalsynthesis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    uiState: AnalysisUiState,
    onIntentSelected: (TradingIntent) -> Unit,
    onAssetClassSelected: (com.polaralias.signalsynthesis.data.settings.AssetClass) -> Unit,
    onDiscoveryModeSelected: (com.polaralias.signalsynthesis.data.settings.DiscoveryMode) -> Unit,
    onRunAnalysis: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenResults: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWatchlist: () -> Unit,
    onOpenHistory: () -> Unit,
    onDismissError: () -> Unit,
    onClearNavigation: () -> Unit,
    onCancelAnalysis: () -> Unit,
    onTogglePause: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(uiState.navigationEvent) {
        if (uiState.navigationEvent is NavigationEvent.Results) {
            onOpenResults()
            onClearNavigation()
        }
    }

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
                        IconButton(onClick = onOpenHistory) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = BrandPrimary)
                        }
                        
                        RainbowMcpText(
                            text = "ANALYSIS",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                        )

                        IconButton(onClick = onOpenWatchlist) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Watchlist", tint = BrandSecondary)
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {


                MockModeBanner(
                    isVisible = !uiState.hasAnyApiKeys,
                    onClick = onOpenKeys
                )

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (uiState.isPaused) {
                        com.polaralias.signalsynthesis.ui.components.GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.PauseCircle, contentDescription = null, tint = Rainbow5, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SYNTHESIS PAUSED", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = Rainbow5)
                                    Text("Background processing suspended.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                TextButton(onClick = onTogglePause) {
                                    Text("RESUME", fontWeight = FontWeight.Black, color = Rainbow5)
                                }
                            }
                        }
                    }

                    SectionHeader("ASSET UNIVERSE")
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            AssetChip(
                                label = "EQUITIES",
                                icon = Icons.Filled.CandlestickChart,
                                selected = uiState.assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.STOCKS,
                                onClick = { onAssetClassSelected(com.polaralias.signalsynthesis.data.settings.AssetClass.STOCKS) }
                            )
                        }
                        item {
                            AssetChip(
                                label = "FOREX",
                                icon = Icons.Filled.CurrencyExchange,
                                selected = uiState.assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.FOREX,
                                onClick = { onAssetClassSelected(com.polaralias.signalsynthesis.data.settings.AssetClass.FOREX) }
                            )
                        }
                        item {
                            AssetChip(
                                label = "METALS",
                                icon = Icons.Filled.Diamond,
                                selected = uiState.assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.METALS,
                                onClick = { onAssetClassSelected(com.polaralias.signalsynthesis.data.settings.AssetClass.METALS) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader("DISCOVERY PROTOCOL")
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AssetChip(
                            label = "CURATED",
                            icon = Icons.Filled.AutoGraph,
                            selected = uiState.appSettings.discoveryMode == com.polaralias.signalsynthesis.data.settings.DiscoveryMode.CURATED,
                            modifier = Modifier.weight(1f),
                            onClick = { onDiscoveryModeSelected(com.polaralias.signalsynthesis.data.settings.DiscoveryMode.CURATED) }
                        )
                        AssetChip(
                            label = "LIVE SCAN",
                            icon = Icons.Filled.Radar,
                            selected = uiState.appSettings.discoveryMode == com.polaralias.signalsynthesis.data.settings.DiscoveryMode.LIVE_SCANNER,
                            modifier = Modifier.weight(1f),
                            onClick = { onDiscoveryModeSelected(com.polaralias.signalsynthesis.data.settings.DiscoveryMode.LIVE_SCANNER) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader("TEMPORAL INTENT")
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IntentChip(
                            label = "DAY",
                            icon = Icons.Filled.Timer,
                            selected = uiState.intent == TradingIntent.DAY_TRADE,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntentSelected(TradingIntent.DAY_TRADE) }
                        )
                        IntentChip(
                            label = "SWING",
                            icon = Icons.Filled.ShowChart,
                            selected = uiState.intent == TradingIntent.SWING,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntentSelected(TradingIntent.SWING) }
                        )
                        IntentChip(
                            label = "POSITION",
                            icon = Icons.Filled.CalendarMonth,
                            selected = uiState.intent == TradingIntent.LONG_TERM,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntentSelected(TradingIntent.LONG_TERM) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader("CONTROL MATRIX")
                    if (uiState.errorMessage != null) {
                        com.polaralias.signalsynthesis.ui.components.GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Rainbow4, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SYSTEM EXCEPTION",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Rainbow4
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = onDismissError) {
                                    Text("CLEAR LOGS", fontWeight = FontWeight.Black, color = Rainbow4)
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.isLoading) {
                            com.polaralias.signalsynthesis.ui.components.GlassBox(
                                modifier = Modifier.weight(1f).height(64.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = BrandPrimary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        uiState.progressMessage?.uppercase() ?: "SYNTHESIZING...",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = BrandPrimary,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            
                            com.polaralias.signalsynthesis.ui.components.GlassBox(
                                modifier = Modifier.size(64.dp).clickable { onCancelAnalysis() }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Rainbow4)
                                }
                            }
                        } else {
                            com.polaralias.signalsynthesis.ui.components.GlassBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clickable(enabled = !uiState.isPaused) { onRunAnalysis() }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.PowerSettingsNew, 
                                            contentDescription = null, 
                                            tint = if (!uiState.isPaused) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "INITIATE SYNTHESIS",
                                            color = if (!uiState.isPaused) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader("ANALYSIS STATISTICS")
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        if (uiState.result == null) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "NO ACTIVE SESSION", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            }
                        } else {
                            val result = uiState.result
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TelemetryItem("CANDIDATES", result.totalCandidates.toString(), Rainbow1)
                                    TelemetryItem("VALID", result.tradeableCount.toString(), Rainbow5)
                                    TelemetryItem("SETUPS", result.setupCount.toString(), Rainbow2)
                                }
                                
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(), 
                                    horizontalArrangement = Arrangement.SpaceBetween, 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("LAST EXECUTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                                        Text(formatTime(uiState.lastRunAt), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                    }
                                    
                                    Button(
                                        onClick = onOpenResults,
                                        colors = ButtonDefaults.buttonColors(containerColor = Rainbow2.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Rainbow2.copy(alpha = 0.2f))
                                    ) {
                                        Text("RESULTS", color = Rainbow2, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun TelemetryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun IntentChip(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = if (selected) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    com.polaralias.signalsynthesis.ui.components.GlassBox(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AssetChip(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = if (selected) BrandSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    com.polaralias.signalsynthesis.ui.components.GlassBox(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
            )
        }
    }
}

