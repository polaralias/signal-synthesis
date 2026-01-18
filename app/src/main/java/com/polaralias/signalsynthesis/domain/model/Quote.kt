package com.polaralias.signalsynthesis.domain.model

import java.time.Instant

data class Quote(
    val symbol: String,
    val price: Double,
    val volume: Long,
    val timestamp: Instant,
    val changePercent: Double? = null
)
