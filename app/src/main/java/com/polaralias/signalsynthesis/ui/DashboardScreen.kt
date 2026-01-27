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
import com.polaralias.signalsynthesis.ui.theme.*

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
    onBlockTicker: (String) -> Unit,
    onOpenWatchlist: () -> Unit // Adding Watchlist navigation
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
                        IconButton(onClick = onRefreshMarket) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Telemetry", tint = BrandPrimary)
                        }
                        
                        RainbowMcpText(
                            text = "SIGNAL SYNTHESIS",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Black)
                        )

                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Tune, contentDescription = "System Config", tint = BrandPrimary)
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
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MockModeBanner(
                        isVisible = !uiState.hasAnyApiKeys,
                        onClick = onOpenSettings
                    )

                    // Market Pulse Section
                    uiState.marketOverview?.let { overview ->
                        MarketSection(
                            overview = overview,
                            isLoading = uiState.isLoadingMarket,
                            onOpenDetail = onOpenDetail
                        )
                    }

                    // Synthesis Protocols Section
                    QuickAnalysisSection(onIntentSelected = onIntentSelected)

                    // Watchlist Preview Section
                    WatchlistPreview(onOpenWatchlist = onOpenWatchlist)

                    // Recent Intelligence Section
                    if (uiState.result != null) {
                        RecentResultsSection(
                            uiState = uiState,
                            onOpenResults = onOpenResults,
                            onOpenDetail = onOpenDetail,
                            onRemoveTicker = onRemoveTicker,
                            onBlockTicker = { symbolToBlock = it }
                        )
                    }

                    // Monitoring Status
                    AlertStatusCard(
                        enabled = uiState.alertsEnabled,
                        count = uiState.alertSymbolCount,
                        onOpenAlertsList = onOpenAlertsList
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
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
private fun WatchlistPreview(onOpenWatchlist: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            SectionHeader(title = "WATCHLIST")
            TextButton(onClick = onOpenWatchlist) {
                Text("MANAGE", style = MaterialTheme.typography.labelSmall, color = BrandPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
        com.polaralias.signalsynthesis.ui.components.GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenWatchlist
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ViewQuilt, contentDescription = null, tint = BrandSecondary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("SECTOR MONITORING", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("View sector performance metrics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
private fun MarketSection(
    overview: MarketOverview,
    isLoading: Boolean,
    onOpenDetail: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            SectionHeader(title = "MARKET DATA")
            if (isLoading) {
                Box(modifier = Modifier.padding(bottom = 12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BrandPrimary)
                }
            } else {
                Text(
                    text = "SYNCED ${formatTime(overview.lastUpdated)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 12.dp),
                    letterSpacing = 1.sp
                )
            }
        }
        
        overview.sections.forEach { section ->
            Column {
                Text(
                    text = section.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = BrandSecondary,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(section.items) { index ->
                        IndexCard(index, onClick = { onOpenDetail(index.symbol) })
                    }
                }
            }
        }
    }
}



@Composable
private fun IndexCard(index: IndexQuote, onClick: () -> Unit) {
    val isUp = index.changePercent >= 0
    val trendColor = if (isUp) BrandPrimary else ErrorRed
    
    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier.width(150.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                index.symbol,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelLarge,
                color = BrandPrimary,
                letterSpacing = 1.sp
            )
            Text(
                index.name.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                formatPrice(index.price),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isUp) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = trendColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${String.format("%.2f", index.changePercent)}%",
                    color = trendColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun QuickAnalysisSection(onIntentSelected: (TradingIntent) -> Unit) {
    Column {
        SectionHeader(title = "TRADING STRATEGIES")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalysisButton("DAY TRADE", BrandPrimary, Icons.Default.FlashOn, Modifier.weight(1f)) { onIntentSelected(TradingIntent.DAY_TRADE) }
            AnalysisButton("SWING", BrandSecondary, Icons.Default.Timeline, Modifier.weight(1f)) { onIntentSelected(TradingIntent.SWING) }
            AnalysisButton("POSITION", RainbowGreen, Icons.Default.Event, Modifier.weight(1f)) { onIntentSelected(TradingIntent.LONG_TERM) }
        }
    }
}

@Composable
private fun AnalysisButton(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    com.polaralias.signalsynthesis.ui.components.GlassBox(
        modifier = modifier
            .height(84.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
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
            verticalAlignment = Alignment.Bottom
        ) {
            SectionHeader(title = "RECENT SIGNALS")
            TextButton(onClick = onOpenResults) {
                Text("HISTORICAL", style = MaterialTheme.typography.labelSmall, color = BrandPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
        if (setups.isEmpty()) {
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "NO ACTIVE SIGNALS FOUND", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
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
                                Text(
                                    setup.symbol, 
                                    fontWeight = FontWeight.Black, 
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BrandPrimary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                SourceBadge(setup.source)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IntentBadge(setup.intent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    setup.setupType.uppercase(), 
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatPercent(setup.confidence), 
                                fontWeight = FontWeight.Black, 
                                style = MaterialTheme.typography.titleLarge,
                                color = if (setup.confidence > 0.7) BrandPrimary else BrandSecondary
                            )
                            Text("RELIABILITY", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        IconButton(modifier = Modifier.size(32.dp), onClick = { onRemoveTicker(setup.symbol) }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Purge", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertStatusCard(enabled: Boolean, count: Int, onOpenAlertsList: () -> Unit) {
    val color = if (enabled) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    
    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenAlertsList
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.1f))
                    .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (enabled) Icons.Filled.Radar else Icons.Filled.SettingsInputAntenna,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = if (enabled) "MONITORING ACTIVE" else "MONITORING PAUSED",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = color,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (enabled) "Monitoring $count assets" else "Tap to resume scanning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}


