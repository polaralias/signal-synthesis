package com.polaralias.signalsynthesis.data.cache

data class CacheTtlConfig(
    val quoteTtlMs: Long = 60_000L,
    val intradayTtlMs: Long = 10 * 60_000L,
    val dailyTtlMs: Long = 24 * 60 * 60_000L,
    val profileTtlMs: Long = 24 * 60 * 60_000L,
    val metricsTtlMs: Long = 24 * 60 * 60_000L,
    val sentimentTtlMs: Long = 30 * 60_000L
) {
    companion object {
        fun fromMinutes(
            quoteMinutes: Int,
            intradayMinutes: Int,
            dailyMinutes: Int,
            profileMinutes: Int,
            metricsMinutes: Int,
            sentimentMinutes: Int
        ): CacheTtlConfig {
            return CacheTtlConfig(
                quoteTtlMs = quoteMinutes.coerceAtLeast(1) * 60_000L,
                intradayTtlMs = intradayMinutes.coerceAtLeast(1) * 60_000L,
                dailyTtlMs = dailyMinutes.coerceAtLeast(1) * 60_000L,
                profileTtlMs = profileMinutes.coerceAtLeast(1) * 60_000L,
                metricsTtlMs = metricsMinutes.coerceAtLeast(1) * 60_000L,
                sentimentTtlMs = sentimentMinutes.coerceAtLeast(1) * 60_000L
            )
        }
    }
}
