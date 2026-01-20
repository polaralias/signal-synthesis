package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    var showRawData by remember { mutableStateOf(false) }
    val aiSummary = uiState.aiSummaries[symbol]

    LaunchedEffect(symbol, uiState.hasLlmKey) {
        if (uiState.hasLlmKey) {
            onRequestSummary(symbol)
        }
        onRequestChartData(symbol)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(symbol)
                        if (setup?.isUserAdded == true) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("👤", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleWatchlist(symbol) }) {
                        Text(if (uiState.watchlist.contains(symbol)) "⭐" else "☆")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (setup == null) {
                Text("Setup not found.")
                return@Scaffold
            }

            SectionHeader("AI Summary")
            when {
                !uiState.hasLlmKey -> Text("Add an LLM key to enable AI synthesis.")
                aiSummary?.status == AiSummaryStatus.LOADING -> Text("Generating AI synthesis...")
                aiSummary?.status == AiSummaryStatus.ERROR -> Text(aiSummary.errorMessage ?: "AI synthesis failed.")
                aiSummary?.status == AiSummaryStatus.READY -> {
                    Text(aiSummary.summary.orEmpty())
                    if (aiSummary.risks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader("Risks")
                        aiSummary.risks.forEach { risk ->
                            Text("- $risk")
                        }
                    }
                    aiSummary.verdict?.let { verdict ->
                        if (verdict.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader("Verdict")
                            Text(verdict)
                        }
                    }
                }
                else -> Text("AI synthesis will appear once generated.")
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

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Price Chart")
            val chartState = uiState.chartData[symbol]
            when (chartState?.status) {
                ChartStatus.LOADING -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Loading chart...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                ChartStatus.READY -> {
                    ChartView(points = chartState.points)
                }
                ChartStatus.ERROR -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(chartState.errorMessage ?: "Failed to load chart", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No chart data.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { showRawData = !showRawData }) {
                Text(if (showRawData) "Hide Raw Data" else "View Raw Data")
            }

            if (showRawData) {
                SectionHeader("Setup Details")
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

                SectionHeader("Technical Indicators")
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

                SectionHeader("Fundamentals & Sentiment")
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

                SectionHeader("Citations & Reasons")
                if (setup.reasons.isEmpty()) {
                    Text("No specific reasons recorded.")
                } else {
                    setup.reasons.forEach { reason ->
                        Text("• $reason", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
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
