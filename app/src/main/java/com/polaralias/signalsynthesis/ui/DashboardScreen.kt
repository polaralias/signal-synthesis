package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.*
import com.polaralias.signalsynthesis.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: AnalysisUiState,
    onIntentSelected: (TradingIntent) -> Unit,
    onRefreshMarket: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenResults: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenAlertsList: () -> Unit,
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
                            text = "SIGNAL SYNTHESIS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                letterSpacing = 4.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    },
                    actions = {
                        IconButton(onClick = onRefreshMarket) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Market", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.secondary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
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
                    onClick = onOpenSettings
                )

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Market Overview Section
                    uiState.marketOverview?.let { overview ->
                        MarketSection(
                            overview = overview,
                            isLoading = uiState.isLoadingMarket
                        )
                    } ?: run {
                        if (uiState.isLoadingMarket) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Provider Blacklist Warning
                    if (uiState.blacklistedProviders.isNotEmpty()) {
                        com.polaralias.signalsynthesis.ui.components.GlassCard(
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🚫", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Providers Paused",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = com.polaralias.signalsynthesis.ui.theme.NeonRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "The following providers are paused: ${uiState.blacklistedProviders.joinToString(", ")}. Tap to check your API keys.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Quick Analysis Section
                    QuickAnalysisSection(onIntentSelected = onIntentSelected)

                    // Recent Results Section
                    if (uiState.result != null) {
                        RecentResultsSection(
                            uiState = uiState,
                            onOpenResults = onOpenResults,
                            onOpenDetail = onOpenDetail,
                            onRemoveTicker = onRemoveTicker,
                            onBlockTicker = { symbolToBlock = it }
                        )
                    }

                    // Alerts Status
                    AlertStatusCard(
                        enabled = uiState.alertsEnabled,
                        count = uiState.alertSymbolCount,
                        onOpenAlertsList = onOpenAlertsList
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
private fun MarketSection(
    overview: MarketOverview,
    isLoading: Boolean
) {
    var selectedIndex by remember { mutableStateOf<IndexQuote?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Market Overview")
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    text = "Updated: ${formatTime(overview.lastUpdated)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        overview.sections.forEach { section ->
            Column {
                Text(
                    text = section.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(section.items) { index ->
                        IndexCard(index, onClick = { selectedIndex = index })
                    }
                }
            }
        }
    }

    if (selectedIndex != null) {
        MarketDetailDialog(
            index = selectedIndex!!,
            onDismiss = { selectedIndex = null }
        )
    }
}

@Composable
private fun MarketDetailDialog(index: IndexQuote, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${index.name} (${index.symbol})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Current Price", formatPrice(index.price))
                DetailRow("Change %", "${if (index.changePercent >= 0) "+" else ""}${String.format("%.2f", index.changePercent)}%")
                index.volume?.let { DetailRow("Volume", formatLargeNumber(it)) }
                index.open?.let { DetailRow("Open", formatPrice(it)) }
                index.high?.let { DetailRow("Day High", formatPrice(it)) }
                index.low?.let { DetailRow("Day Low", formatPrice(it)) }
                index.previousClose?.let { DetailRow("Prev Close", formatPrice(it)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IndexCard(index: IndexQuote, onClick: () -> Unit) {
    val trendColor = if (index.changePercent >= 0) com.polaralias.signalsynthesis.ui.theme.NeonGreen else com.polaralias.signalsynthesis.ui.theme.NeonRed
    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier.width(140.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                index.symbol,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                index.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                formatPrice(index.price),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${if (index.changePercent >= 0) "▲" else "▼"} ${String.format("%.2f", index.changePercent)}%",
                color = trendColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickAnalysisSection(onIntentSelected: (TradingIntent) -> Unit) {
    Column {
        SectionHeader(title = "AI Engine")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalysisButton("DAY", com.polaralias.signalsynthesis.ui.theme.NeonBlue, Modifier.weight(1f)) { onIntentSelected(TradingIntent.DAY_TRADE) }
            AnalysisButton("SWING", com.polaralias.signalsynthesis.ui.theme.NeonPurple, Modifier.weight(1f)) { onIntentSelected(TradingIntent.SWING) }
            AnalysisButton("LONG", com.polaralias.signalsynthesis.ui.theme.NeonGreen, Modifier.weight(1f)) { onIntentSelected(TradingIntent.LONG_TERM) }
        }
    }
}

@Composable
private fun AnalysisButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.2f),
                        color.copy(alpha = 0.05f)
                    )
                )
            )
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun RecentResultsSection(
    uiState: AnalysisUiState,
    onOpenResults: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onRemoveTicker: (String) -> Unit,
    onBlockTicker: (String) -> Unit
) {
    val setups = uiState.result?.setups?.filter { !uiState.removedAlerts.contains(it.symbol) }?.take(3) ?: emptyList()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Recent Insights")
            TextButton(onClick = onOpenResults) {
                Text("DISCOVER ALL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        if (setups.isEmpty()) {
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "No setups found in last run.", 
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            setups.forEach { setup ->
                com.polaralias.signalsynthesis.ui.components.GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { onOpenDetail(setup.symbol) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(setup.symbol, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(12.dp))
                                SourceBadge(setup.source)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IntentBadge(setup.intent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    setup.setupType, 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatPercent(setup.confidence), 
                                fontWeight = FontWeight.ExtraBold, 
                                style = MaterialTheme.typography.titleMedium,
                                color = if (setup.confidence > 0.7) com.polaralias.signalsynthesis.ui.theme.NeonGreen else MaterialTheme.colorScheme.primary
                            )
                            Text("CONFIDENCE", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        IconButton(modifier = Modifier.size(32.dp), onClick = { onRemoveTicker(setup.symbol) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertStatusCard(enabled: Boolean, count: Int, onOpenAlertsList: () -> Unit) {
    val color = if (enabled) com.polaralias.signalsynthesis.ui.theme.NeonBlue else com.polaralias.signalsynthesis.ui.theme.MediumGray
    
    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenAlertsList
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f))
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (enabled) "🔔" else "🔕", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = if (enabled) "MARKET ALERTS ACTIVE" else "MARKET ALERTS PAUSED",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (enabled) "Monitoring $count premium symbols" else "Tap to enable and stay updated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color.copy(alpha = 0.5f)
            )
        }
    }
}
