package com.polaralias.signalsynthesis.domain.model

import java.time.Instant

data class IndexQuote(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
    val volume: Long? = null,
    val high: Double? = null,
    val low: Double? = null,
    val open: Double? = null,
    val previousClose: Double? = null
)

data class MarketSection(
    val title: String,
    val items: List<IndexQuote>
)

data class MarketOverview(
    val sections: List<MarketSection>,
    val lastUpdated: Instant
)
