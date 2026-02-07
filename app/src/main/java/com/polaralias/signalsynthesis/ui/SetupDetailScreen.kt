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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polaralias.signalsynthesis.domain.model.*
import com.polaralias.signalsynthesis.data.rss.RssFeedDefaults
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.polaralias.signalsynthesis.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupDetailScreen(
    uiState: AnalysisUiState,
    symbol: String,
    onBack: () -> Unit,
    onRequestSummary: (String) -> Unit,
    onRequestDeepDive: (String) -> Unit,
    onRequestChartData: (String) -> Unit,
    onToggleWatchlist: (String) -> Unit
) {
    val setup = uiState.result?.setups?.firstOrNull { it.symbol == symbol }
        ?: uiState.history.flatMap { it.setups }.firstOrNull { it.symbol == symbol }
    
    var showRawData by remember { mutableStateOf(false) }
    val aiSummary = uiState.aiSummaries[symbol]

    LaunchedEffect(symbol, uiState.hasLlmKey) {
        if (uiState.hasLlmKey) {
            onRequestSummary(symbol)
        }
        onRequestChartData(symbol)
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
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandPrimary)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RainbowMcpText(
                                text = symbol, 
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp)
                            )
                            setup?.let {
                                Spacer(modifier = Modifier.width(16.dp))
                                SourceBadge(it.source)
                            }
                        }

                        IconButton(onClick = { onToggleWatchlist(symbol) }) {
                            Icon(
                                imageVector = if (uiState.watchlist.contains(symbol)) Icons.Filled.Star else Icons.Default.StarBorder,
                                contentDescription = "Watchlist",
                                tint = if (uiState.watchlist.contains(symbol)) WarningOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
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
                    .verticalScroll(rememberScrollState())
            ) {
                if (setup == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ASSET NOT FOUND", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontWeight = FontWeight.Black)
                    }
                    return@Scaffold
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {


                    SectionHeader("AI INTERPRETATION")
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            when {
                                !uiState.hasLlmKey -> {
                                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                        Text("AI service offline. Activate LLM in settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), textAlign = TextAlign.Center)
                                    }
                                }
                                aiSummary?.status == AiSummaryStatus.LOADING -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = BrandPrimary)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("SYNTHESIZING DATA STREAM...", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary, letterSpacing = 2.sp)
                                    }
                                }
                                aiSummary?.status == AiSummaryStatus.ERROR -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(aiSummary.errorMessage ?: "ANALYSIS FAILED", color = ErrorRed, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                aiSummary?.status == AiSummaryStatus.READY -> {
                                    Text(
                                        aiSummary.summary.orEmpty(), 
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    
                                    if (aiSummary.risks.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text("RISK PARAMETERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Rainbow4, letterSpacing = 2.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        aiSummary.risks.forEach { risk ->
                                            Row(modifier = Modifier.padding(vertical = 6.dp)) {
                                                Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(14.dp).padding(top = 2.dp), tint = Rainbow4.copy(alpha = 0.5f))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(risk, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                        }
                                    }
                                    
                                    aiSummary.verdict?.let { verdict ->
                                        if (verdict.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Text("SYSTEM VERDICT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandSecondary, letterSpacing = 2.sp)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(BrandSecondary.copy(alpha = 0.05f))
                                                    .border(1.dp, BrandSecondary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                                    .padding(16.dp)
                                            ) {
                                                Text(verdict, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = BrandSecondary)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                        Text("Initializing AI service...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }

                    SectionHeader("DEEP DIVE (WEB SEARCH)")
                    val deepDive = uiState.deepDives[symbol]
                    val expandedTopicsEnabled = uiState.appSettings.rssEnabledTopics.any { key ->
                        RssFeedDefaults.expandedTopicKeys.contains(key)
                    }
                    val expandedActive = expandedTopicsEnabled && (uiState.appSettings.rssApplyExpandedToAll || setup.expandedRssNeeded)
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            if (expandedActive) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Public, contentDescription = null, tint = BrandSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("EXPANDED FEEDS USED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandSecondary, letterSpacing = 1.sp)
                                }
                                setup.expandedRssReason?.takeIf { it.isNotBlank() }?.let { reason ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            when (deepDive?.status) {
                                DeepDiveStatus.LOADING -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = BrandSecondary)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("SCANNING GLOBAL NEWS & DATA...", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandSecondary, letterSpacing = 2.sp)
                                    }
                                }
                                DeepDiveStatus.READY -> {
                                    deepDive.data?.let { data ->
                                        Text(data.summary, style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)
                                        
                                        if (data.drivers.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Text("PRIMARY DRIVERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Rainbow2, letterSpacing = 2.sp)
                                            data.drivers.forEach { driver ->
                                                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                                    val icon = if (driver.direction.contains("bull", ignoreCase = true)) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown
                                                    val color = if (driver.direction.contains("bull", ignoreCase = true)) SuccessGreen else if (driver.direction.contains("bear", ignoreCase = true)) ErrorRed else BrandPrimary
                                                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(driver.type.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = color.copy(alpha = 0.7f))
                                                        Text(driver.detail, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                            }
                                        }

                                        if (data.sources.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Text("SOURCES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary, letterSpacing = 2.sp)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                                            data.sources.forEach { source ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp)
                                                        .clickable { if (source.url.isNotBlank()) uriHandler.openUri(source.url) },
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandPrimary.copy(alpha = 0.5f))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(source.title, style = MaterialTheme.typography.bodySmall, color = BrandPrimary, fontWeight = FontWeight.Bold)
                                                        Text("${source.publisher} • ${source.publishedAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { onRequestDeepDive(symbol) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandSecondary.copy(alpha = 0.1f)),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, BrandSecondary.copy(alpha = 0.2f))
                                        ) {
                                            Text("REFRESH DEEP DIVE", color = BrandSecondary, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                DeepDiveStatus.ERROR -> {
                                    Text(deepDive.errorMessage ?: "Deep dive failed", color = ErrorRed)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { onRequestDeepDive(symbol) }, modifier = Modifier.fillMaxWidth()) {
                                        Text("RETRY")
                                    }
                                }
                                else -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        Text("Execute targeted web search and recent news analysis for this ticker.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { onRequestDeepDive(symbol) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandSecondary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("INITIATE DEEP DIVE", fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    setup.metrics?.earningsDate?.let { date ->
                        val daysUntil = try {
                            val earningsLocalDate = java.time.LocalDate.parse(date.take(10))
                            val today = java.time.LocalDate.now()
                            java.time.temporal.ChronoUnit.DAYS.between(today, earningsLocalDate)
                        } catch (e: Exception) {
                            com.polaralias.signalsynthesis.util.Logger.w("SetupDetailScreen", "Failed to parse earnings date: $date", e)
                            null
                        }

                        if (daysUntil != null && daysUntil in 0..3) {
                            Spacer(modifier = Modifier.height(16.dp))
                            com.polaralias.signalsynthesis.ui.components.GlassBox(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.EventRepeat, contentDescription = null, tint = Rainbow4, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "EARNINGS PREDICTION: $daysUntil DAYS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Rainbow4,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    SectionHeader("MARKET SPECTRUM")
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        val chartState = uiState.chartData[symbol]
                        when (chartState?.status) {
                            ChartStatus.LOADING -> {
                                Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = BrandPrimary, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                                }
                            }
                            ChartStatus.READY -> {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    ChartView(
                                        points = chartState.points,
                                        vwap = setup.intradayStats?.vwap
                                    )
                                }
                            }
                            ChartStatus.ERROR -> {
                                Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.SignalCellularConnectedNoInternet0Bar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(chartState.errorMessage ?: "TELEMETRY FAILURE", color = ErrorRed.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    }
                                }
                            }
                            else -> {
                                Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                                    Text("OFFLINE TELEMETRY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    com.polaralias.signalsynthesis.ui.components.GlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { showRawData = !showRawData }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (showRawData) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, 
                                    contentDescription = null, 
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    if (showRawData) "CONCEAL METRICS" else "REVEAL FULL SPECTRUM", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    fontWeight = FontWeight.Black, 
                                    color = BrandPrimary,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }

                    if (showRawData) {
                        Spacer(modifier = Modifier.height(32.dp))
                        SectionHeader("QUANTITATIVE")
                        com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                EducationalMetricRow(MetricInfo(
                                    label = "Archetype",
                                    value = setup.setupType,
                                    description = "The specific market structure or statistical edge identified.",
                                    relationship = "Each archetype possesses unique historical reliability and risk profiles."
                                ))
                                EducationalMetricRow(MetricInfo(
                                    label = "Reliability Index",
                                    value = formatPercent(setup.confidence),
                                    description = "A synthetic score aggregating technical, fundamental, and sentiment signals.",
                                    relationship = "Higher values indicate stronger confluence across the data spectrum."
                                ))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader("TECHNICAL SIGNALS")
                        com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                setup.intradayStats?.let { stats ->
                                    stats.rsi14?.let { rsi ->
                                        val status = when {
                                            rsi < 30 -> "Oversold"
                                            rsi > 70 -> "Overbought"
                                            else -> "Neutral"
                                        }
                                        EducationalMetricRow(MetricInfo(
                                            label = "RSI (14)",
                                            value = String.format(Locale.US, "%.2f", rsi),
                                            status = status,
                                            description = "Speed and change of price oscillations on a 0-100 scale.",
                                            relationship = "Extremes often signal impending reversals or momentum exhaustion."
                                        ))
                                    }
                                    stats.vwap?.let { vwap ->
                                        val status = if (setup.triggerPrice > vwap) "Bullish Trend" else "Bearish Trend"
                                        EducationalMetricRow(MetricInfo(
                                            label = "VWAP",
                                            value = formatPrice(vwap),
                                            status = status,
                                            description = "Average price weighted by volume throughout the session.",
                                            relationship = "Price relative to VWAP indicates institutional dominance and intraday health."
                                        ))
                                    }
                                    stats.atr14?.let { atr ->
                                        EducationalMetricRow(MetricInfo(
                                            label = "ATR (14)",
                                            value = formatPrice(atr),
                                            description = "Average True Range measures the expected move per period.",
                                            relationship = "Higher ATR mandates wider protection zones to avoid market noise."
                                        ))
                                    }
                                }

                                setup.eodStats?.let { eod ->
                                    eod.sma200?.let { sma ->
                                        val status = if (setup.triggerPrice > sma) "Bullish" else "Bearish"
                                        EducationalMetricRow(MetricInfo(
                                            label = "SMA (200)",
                                            value = formatPrice(sma),
                                            status = status,
                                            description = "The primary long-term trend filter used by global institutions.",
                                            relationship = "Acts as a critical structural support or resistance level."
                                        ))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader("FUNDAMENTAL MATRIX")
                        com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                setup.profile?.let { profile ->
                                    EducationalMetricRow(MetricInfo(
                                        label = "Sector Hub",
                                        value = "${profile.sector ?: "N/A"} / ${profile.industry ?: "N/A"}",
                                        description = "The economic sector and industry of operation.",
                                        relationship = "Sector rotations can override individual technical setups."
                                    ))
                                }
                                setup.metrics?.let { metrics ->
                                    metrics.peRatio?.let { pe ->
                                        EducationalMetricRow(MetricInfo(
                                            label = "P/E Ratio",
                                            value = String.format(Locale.US, "%.2f", pe),
                                            description = "Price-to-Earnings valuation multiplier.",
                                            relationship = "Indicates market expectations for future growth or contraction."
                                        ))
                                    }
                                    metrics.dividendYield?.let { yield ->
                                        EducationalMetricRow(MetricInfo(
                                            label = "Dividend Yield",
                                            value = String.format(Locale.US, "%.2f%%", yield * 100),
                                            description = "Percentage of share price paid out annually.",
                                            relationship = "Provides a structural safety net for capital preservation."
                                        ))
                                    }
                                }
                                setup.sentiment?.let { sentiment ->
                                    sentiment.score?.let { score ->
                                        val status = when {
                                            score > 0.2 -> "Bullish"
                                            score < -0.2 -> "Bearish"
                                            else -> "Neutral"
                                        }
                                        EducationalMetricRow(MetricInfo(
                                            label = "Crowd Sentiment",
                                            value = String.format(Locale.US, "%.2f", score),
                                            status = status,
                                            description = "Aggregated bias from global news and social stream.",
                                            relationship = "Crowd behavior acts as momentum fuel or a contrarian signal."
                                        ))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader("SYSTEM LOGIC")
                        com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                if (setup.reasons.isEmpty()) {
                                    Text("NO KEY FACTORS DETECTED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                } else {
                                    setup.reasons.forEach { reason ->
                                        Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                            Icon(Icons.Filled.Terminal, contentDescription = null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = BrandPrimary.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), lineHeight = 22.sp)
                                        }
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

private fun formatValidUntil(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}
