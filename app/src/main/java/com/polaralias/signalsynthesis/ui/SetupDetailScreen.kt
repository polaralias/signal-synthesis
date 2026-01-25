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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polaralias.signalsynthesis.domain.model.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.polaralias.signalsynthesis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupDetailScreen(
    uiState: AnalysisUiState,
    symbol: String,
    onBack: () -> Unit,
    onRequestSummary: (String) -> Unit,
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
                TopAppBar(
                    title = { 
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            RainbowMcpText(
                                text = symbol, 
                                style = MaterialTheme.typography.titleLarge
                            )
                            setup?.let {
                                Spacer(modifier = Modifier.width(12.dp))
                                SourceBadge(it.source)
                            }
                        }
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
                    actions = {
                        IconButton(onClick = { onToggleWatchlist(symbol) }) {
                            Text(if (uiState.watchlist.contains(symbol)) "⭐" else "☆", fontSize = 20.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
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
                    .verticalScroll(rememberScrollState())
            ) {
                if (setup == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Setup not found.", style = MaterialTheme.typography.titleMedium)
                    }
                    return@Scaffold
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            SectionHeader("AI SYNTHESIS")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    when {
                        !uiState.hasLlmKey -> Text("Connect an AI model in settings to unlock synthesis.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        aiSummary?.status == AiSummaryStatus.LOADING -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("SYNTHESIZING MARKET DATA...", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        aiSummary?.status == AiSummaryStatus.ERROR -> Text(aiSummary.errorMessage ?: "AI NODE FAILURE", color = RainbowRed, fontWeight = FontWeight.Black)
                        aiSummary?.status == AiSummaryStatus.READY -> {
                            Text(
                                aiSummary.summary.orEmpty(), 
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                            
                            if (aiSummary.risks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text("IDENTIFIED RISKS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = com.polaralias.signalsynthesis.ui.theme.NeonRed, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                aiSummary.risks.forEach { risk ->
                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("•", color = RainbowRed, fontWeight = FontWeight.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(risk, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                    }
                                }
                            }
                            
                            aiSummary.verdict?.let { verdict ->
                                if (verdict.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text("SYNTHESIS VERDICT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = com.polaralias.signalsynthesis.ui.theme.NeonPurple, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(RainbowPurple.copy(alpha = 0.15f))
                                            .border(1.dp, RainbowPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(verdict, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = RainbowPurple)
                                    }
                                }
                            }
                        }
                        else -> Text("AI nodes are initializing...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            setup.metrics?.earningsDate?.let { date ->
                val daysUntil = try {
                    val earningsLocalDate = java.time.LocalDate.parse(date.take(10))
                    val today = java.time.LocalDate.now()
                    java.time.temporal.ChronoUnit.DAYS.between(today, earningsLocalDate)
                } catch (_: Exception) {
                    null
                }

                if (daysUntil != null && daysUntil in 0..3) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "⚠️ Earnings Alert: Expected in $daysUntil days",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("MARKET SPECTRUM")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                val chartState = uiState.chartData[symbol]
                when (chartState?.status) {
                    ChartStatus.LOADING -> {
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                            Text(chartState.errorMessage ?: "TELEMETRY ERROR", color = com.polaralias.signalsynthesis.ui.theme.NeonRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                            Text("NO TELEMETRY DATA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RainbowBlue.copy(alpha = 0.15f))
                        .border(1.dp, RainbowBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { showRawData = !showRawData },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (showRawData) "CONCEAL RAW DATA" else "REVEAL RAW DATA", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Black, 
                        color = RainbowBlue,
                        letterSpacing = 2.sp
                    )
                }

            if (showRawData) {
                SectionHeader("QUANTITATIVE PARAMETERS")
                com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        EducationalMetricRow(MetricInfo(
                            label = "Setup Type",
                            value = setup.setupType,
                            description = "The visual pattern or statistical condition identified by our scanner.",
                            relationship = "Different setups have different historical win rates and volatility expectations."
                        ))
                        EducationalMetricRow(MetricInfo(
                            label = "Confidence Score",
                            value = formatPercent(setup.confidence),
                            description = "A weighted score based on how many technical and fundamental factors align.",
                            relationship = "Higher confidence indicates stronger confluence between different data points."
                        ))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("TECHNICAL INDICATORS")
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
                                    value = String.format("%.2f", rsi),
                                    status = status,
                                    description = "Relative Strength Index measures the speed and change of price movements on a 0-100 scale.",
                                    relationship = "Values below 30 suggest the stock may be oversold (buying opportunity), while above 70 suggests it's overbought."
                                ))
                            }
                            stats.vwap?.let { vwap ->
                                val status = if (setup.triggerPrice > vwap) "Bullish Trend" else "Bearish Trend"
                                EducationalMetricRow(MetricInfo(
                                    label = "VWAP",
                                    value = formatPrice(vwap),
                                    status = status,
                                    description = "Volume Weighted Average Price gives the average price a stock has traded at throughout the day, based on both volume and price.",
                                    relationship = "Trading above VWAP is often a sign of bullish intraday sentiment and institutional buying."
                                ))
                            }
                            stats.atr14?.let { atr ->
                                EducationalMetricRow(MetricInfo(
                                    label = "ATR (14)",
                                    value = formatPrice(atr),
                                    description = "Average True Range measures market volatility by decomposing the entire range of an asset price for that period.",
                                    relationship = "Higher ATR implies greater volatility, meaning you may need a wider stop-loss to avoid being 'stopped out' by noise."
                                ))
                            }
                        }

                        setup.eodStats?.let { eod ->
                            eod.sma200?.let { sma ->
                                val status = if (setup.triggerPrice > sma) "Long-term Bullish" else "Long-term Bearish"
                                EducationalMetricRow(MetricInfo(
                                    label = "SMA (200)",
                                    value = formatPrice(sma),
                                    status = status,
                                    description = "The 200-day Simple Moving Average is a key indicator used by traders to identify long-term trends.",
                                    relationship = "Many institutional investors use the 200-day SMA as a 'line in the sand' for long-term health."
                                ))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("FUNDAMENTAL MATRIX")
                com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        setup.profile?.let { profile ->
                            EducationalMetricRow(MetricInfo(
                                label = "Sector/Industry",
                                value = "${profile.sector ?: "N/A"} / ${profile.industry ?: "N/A"}",
                                description = "The economic sector and industry the company operates in.",
                                relationship = "Sector rotation and industry-specific news can override individual technical setups."
                            ))
                        }
                        setup.metrics?.let { metrics ->
                            metrics.peRatio?.let { pe ->
                                EducationalMetricRow(MetricInfo(
                                    label = "P/E Ratio",
                                    value = String.format("%.2f", pe),
                                    description = "Price-to-Earnings ratio measures a company's current share price relative to its per-share earnings.",
                                    relationship = "A high P/E might suggest growth expectations or overvaluation; a low P/E might suggest value or decline."
                                ))
                            }
                            metrics.pbRatio?.let { pb ->
                                EducationalMetricRow(MetricInfo(
                                    label = "P/B Ratio",
                                    value = String.format("%.2f", pb),
                                    description = "Price-to-Book ratio compares a firm's market capitalization to its book value.",
                                    relationship = "Often used to find undervalued stocks in 'value' investing. A ratio under 1.0 could indicate an undervalued stock."
                                ))
                            }
                            metrics.debtToEquity?.let { de ->
                                EducationalMetricRow(MetricInfo(
                                    label = "Debt-to-Equity",
                                    value = String.format("%.2f", de),
                                    description = "Measures a company's financial leverage calculated by dividing its total liabilities by its stockholder equity.",
                                    relationship = "High debt-to-equity ratios can be risky, especially in volatile markets or high-interest-rate environments."
                                ))
                            }
                            metrics.dividendYield?.let { yield ->
                                EducationalMetricRow(MetricInfo(
                                    label = "Dividend Yield",
                                    value = String.format("%.2f%%", yield * 100),
                                    description = "A financial ratio that tells you the percentage of a company's share price that it pays out in dividends each year.",
                                    relationship = "Yields provide a 'safety net' or passive income component to long-term stock ownership."
                                ))
                            }
                            metrics.earningsDate?.let { earnings ->
                                EducationalMetricRow(MetricInfo(
                                    label = "Upcoming Earnings",
                                    value = earnings.take(10),
                                    description = "The scheduled date for the next quarterly earnings report.",
                                    relationship = "Earnings reports are major catalysts that can cause extreme price gaps and volatility."
                                ))
                            }
                        }
                        setup.sentiment?.let { sentiment ->
                            sentiment.score?.let { score ->
                                val status = when {
                                    score > 0.2 -> "Bullish Sentiment"
                                    score < -0.2 -> "Bearish Sentiment"
                                    else -> "Neutral Sentiment"
                                }
                                EducationalMetricRow(MetricInfo(
                                    label = "Sentiment Score",
                                    value = String.format("%.2f", score),
                                    status = status,
                                    description = "Aggregated sentiment from recent news and social media, normalized between -1.0 and 1.0.",
                                    relationship = "Crowd sentiment can be a leading indicator of momentum or a contrarian signal at extremes."
                                ))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("CITATIONS & CORE LOGIC")
                com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (setup.reasons.isEmpty()) {
                            Text("No specific logical nodes recorded.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        } else {
                            setup.reasons.forEach { reason ->
                                Row(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Text("⚡", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
    }
}

private fun formatValidUntil(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}
