package com.signalsynthesis.domain.model

import java.time.Instant

data class IntradayBar(
    val time: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)
