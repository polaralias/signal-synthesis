package com.signalsynthesis.domain.model

import java.time.Instant

data class AnalysisResult(
    val intent: TradingIntent,
    val totalCandidates: Int,
    val tradeableCount: Int,
    val setupCount: Int,
    val setups: List<TradeSetup>,
    val generatedAt: Instant
)
