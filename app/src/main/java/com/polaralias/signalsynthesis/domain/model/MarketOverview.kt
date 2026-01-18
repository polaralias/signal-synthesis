package com.polaralias.signalsynthesis.domain.model

import java.time.Instant

data class IndexQuote(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double
)

data class MarketOverview(
    val indices: List<IndexQuote>,
    val lastUpdated: Instant
)
