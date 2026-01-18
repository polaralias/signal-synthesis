package com.polaralias.signalsynthesis.data.settings

data class AppSettings(
    val quoteRefreshIntervalMinutes: Int = 5,
    val alertCheckIntervalMinutes: Int = 15,
    val vwapDipPercent: Double = 1.0,
    val rsiOversold: Double = 30.0,
    val rsiOverbought: Double = 70.0,
    val useMockDataWhenOffline: Boolean = true
)
