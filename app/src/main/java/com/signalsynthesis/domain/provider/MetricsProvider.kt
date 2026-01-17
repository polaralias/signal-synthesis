package com.signalsynthesis.domain.provider

import com.signalsynthesis.domain.model.FinancialMetrics

interface MetricsProvider {
    suspend fun getMetrics(symbol: String): FinancialMetrics?
}
