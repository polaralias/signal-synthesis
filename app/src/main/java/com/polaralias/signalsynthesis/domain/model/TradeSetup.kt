package com.polaralias.signalsynthesis.domain.model

import java.time.Instant

enum class TickerSource {
    PREDEFINED,
    SCREENER,
    CUSTOM,
    LIVE_GAINER,
    LIVE_LOSER,
    LIVE_ACTIVE
}

data class TradeSetup(
    val symbol: String,
    val setupType: String,
    val triggerPrice: Double,
    val stopLoss: Double,
    val targetPrice: Double,
    val confidence: Double,
    val reasons: List<String>,
    val validUntil: Instant,
    val intent: TradingIntent,
    val intradayStats: IntradayStats? = null,
    val eodStats: EodStats? = null,
    val profile: CompanyProfile? = null,
    val metrics: FinancialMetrics? = null,
    val sentiment: SentimentData? = null,
    val source: TickerSource = TickerSource.PREDEFINED
)
