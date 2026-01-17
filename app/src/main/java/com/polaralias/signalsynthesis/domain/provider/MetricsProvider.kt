package com.polaralias.signalsynthesis.domain.provider

import com.polaralias.signalsynthesis.domain.model.FinancialMetrics

interface MetricsProvider {
    suspend fun getMetrics(symbol: String): FinancialMetrics?
}
