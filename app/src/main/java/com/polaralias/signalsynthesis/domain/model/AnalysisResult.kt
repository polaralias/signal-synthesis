package com.polaralias.signalsynthesis.domain.model

import java.time.Instant

data class AnalysisResult(
    val intent: TradingIntent,
    val totalCandidates: Int,
    val tradeableCount: Int,
    val setupCount: Int,
    val setups: List<TradeSetup>,
    val generatedAt: Instant,
    val globalNotes: List<String> = emptyList(),
    val rssDigest: RssDigest? = null,
    val decisionUpdate: DecisionUpdate? = null,
    val fundamentalsNewsSynthesis: FundamentalsNewsSynthesis? = null
)
