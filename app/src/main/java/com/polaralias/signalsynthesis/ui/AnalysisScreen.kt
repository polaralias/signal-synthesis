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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        RainbowMcpText(
                            text = "SYNTHESIS ENGINE",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        val pauseLabel = if (uiState.isPaused) "RESUME" else "PAUSE"
                        val pauseIcon = if (uiState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause
                        TextButton(onClick = onTogglePause) {
                            Icon(pauseIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(pauseLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onOpenWatchlist) {
                            Icon(Icons.Filled.Star, contentDescription = "Watchlist", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = onOpenHistory) {
                            Icon(Icons.Filled.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onSurface)
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

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (uiState.isPaused) {
                        com.polaralias.signalsynthesis.ui.components.GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = null, tint = com.polaralias.signalsynthesis.ui.theme.NeonPurple, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SYSTEM PAUSED", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = com.polaralias.signalsynthesis.ui.theme.NeonPurple)
                                    Text("Background intelligence is currently suspended.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                TextButton(onClick = onTogglePause) {
                                    Text("RESUME", fontWeight = FontWeight.ExtraBold, color = com.polaralias.signalsynthesis.ui.theme.NeonPurple)
                                }
                            }
                        }
                    }

            SectionHeader("ASSET UNIVERSE")
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    AssetChip(
                        label = "STOCKS",
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
                item {
                    AssetChip(
                        label = "ALL",
                        icon = Icons.Filled.Public,
                        selected = uiState.assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.ALL,
                        onClick = { onAssetClassSelected(com.polaralias.signalsynthesis.data.settings.AssetClass.ALL) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("DISCOVERY PROTOCOL")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssetChip(
                    label = "CURATED",
                    icon = Icons.Filled.Recommend,
                    selected = uiState.appSettings.discoveryMode == com.polaralias.signalsynthesis.data.settings.DiscoveryMode.CURATED,
                    onClick = { onDiscoveryModeSelected(com.polaralias.signalsynthesis.data.settings.DiscoveryMode.CURATED) }
                )
                AssetChip(
                    label = "LIVE SCANNER",
                    icon = Icons.Filled.Radar,
                    selected = uiState.appSettings.discoveryMode == com.polaralias.signalsynthesis.data.settings.DiscoveryMode.LIVE_SCANNER,
                    onClick = { onDiscoveryModeSelected(com.polaralias.signalsynthesis.data.settings.DiscoveryMode.LIVE_SCANNER) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("TRADING INTENT")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IntentChip(
                    label = "DAY TRADE",
                    icon = Icons.Filled.Timer,
                    selected = uiState.intent == TradingIntent.DAY_TRADE,
                    onClick = { onIntentSelected(TradingIntent.DAY_TRADE) }
                )
                IntentChip(
                    label = "SWING",
                    icon = Icons.Filled.ShowChart,
                    selected = uiState.intent == TradingIntent.SWING,
                    onClick = { onIntentSelected(TradingIntent.SWING) }
                )
                IntentChip(
                    label = "LONG TERM",
                    icon = Icons.Filled.CalendarToday,
                    selected = uiState.intent == TradingIntent.LONG_TERM,
                    onClick = { onIntentSelected(TradingIntent.LONG_TERM) }
                )
            }

            if (uiState.blacklistedProviders.isNotEmpty()) {
                androidx.compose.material3.Surface(
                    onClick = onOpenKeys,
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Providers Paused",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            "One or more providers are temporarily paused due to authentication errors. Tap to check your API keys.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            SectionHeader("NODE AUTHENTICATION")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RainbowBlue.copy(alpha = 0.15f))
                        .border(1.dp, RainbowBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onOpenKeys() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (uiState.hasAnyApiKeys) "PROTOCOL ACTIVE" else "CONFIG REQUIRED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = RainbowBlue)
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RainbowPurple.copy(alpha = 0.15f))
                        .border(1.dp, RainbowPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("SYSTEM SETTINGS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = RainbowPurple)
                }
            }

            SectionHeader("CONTROL UNIT")
            if (uiState.errorMessage != null) {
                com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠ SYSTEM ERROR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.polaralias.signalsynthesis.ui.theme.NeonRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = onDismissError) {
                            Text("DISMISS", fontWeight = FontWeight.Bold, color = com.polaralias.signalsynthesis.ui.theme.NeonRed)
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.isLoading) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(com.polaralias.signalsynthesis.ui.theme.NeonBlue.copy(alpha = 0.15f))
                            .border(1.dp, com.polaralias.signalsynthesis.ui.theme.NeonBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = androidx.compose.ui.Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = com.polaralias.signalsynthesis.ui.theme.NeonBlue
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                uiState.progressMessage?.uppercase() ?: "PROCESSING...",
                                style = MaterialTheme.typography.labelLarge,
                                color = com.polaralias.signalsynthesis.ui.theme.NeonBlue,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (!uiState.isPaused) com.polaralias.signalsynthesis.ui.theme.NeonBlue.copy(alpha = 0.2f)
                                else com.polaralias.signalsynthesis.ui.theme.MediumGray.copy(alpha = 0.1f)
                            )
                            .border(
                                1.dp, 
                                if (!uiState.isPaused) com.polaralias.signalsynthesis.ui.theme.NeonBlue.copy(alpha = 0.5f)
                                else com.polaralias.signalsynthesis.ui.theme.MediumGray.copy(alpha = 0.3f), 
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !uiState.isPaused) { onRunAnalysis() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "INITIATE ANALYSIS",
                            color = if (!uiState.isPaused) com.polaralias.signalsynthesis.ui.theme.NeonBlue else com.polaralias.signalsynthesis.ui.theme.TextSecondary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                if (uiState.isLoading) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .weight(0.4f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RainbowRed.copy(alpha = 0.1f))
                            .border(1.dp, RainbowRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { onCancelAnalysis() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("STOP", color = RainbowRed, fontWeight = FontWeight.Black)
                    }
                }
            }

            SectionHeader("TELEMETRY SUMMARY")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                if (uiState.result == null) {
                    Text(
                        "NO DATA AVAILABLE", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(24.dp),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                } else {
                    val result = uiState.result
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TelemetryItem("CANDIDATES", result.totalCandidates.toString(), com.polaralias.signalsynthesis.ui.theme.NeonBlue)
                            TelemetryItem("TRADEABLE", result.tradeableCount.toString(), com.polaralias.signalsynthesis.ui.theme.NeonPurple)
                            TelemetryItem("SETUPS", result.setupCount.toString(), com.polaralias.signalsynthesis.ui.theme.NeonGreen)
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("LAST EXECUTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(formatTime(uiState.lastRunAt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.polaralias.signalsynthesis.ui.theme.NeonGreen.copy(alpha = 0.2f))
                                    .clickable { onOpenResults() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("VIEW RESULTS", color = com.polaralias.signalsynthesis.ui.theme.NeonGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}
}

@Composable
private fun TelemetryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun IntentChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) com.polaralias.signalsynthesis.ui.theme.NeonBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (selected) color.copy(alpha = 0.5f) else color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AssetChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) com.polaralias.signalsynthesis.ui.theme.NeonPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (selected) color.copy(alpha = 0.5f) else color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
