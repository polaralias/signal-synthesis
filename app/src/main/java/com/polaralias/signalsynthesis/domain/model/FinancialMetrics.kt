package com.polaralias.signalsynthesis.domain.model

data class FinancialMetrics(
    val marketCap: Long?,
    val peRatio: Double?,
    val eps: Double?,
    val earningsDate: String? = null,
    val dividendYield: Double? = null,
    val pbRatio: Double? = null,
    val debtToEquity: Double? = null
)
