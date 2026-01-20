package com.polaralias.signalsynthesis.domain.provider

interface ScreenerProvider {
    suspend fun screenStocks(
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minVolume: Long? = null,
        sector: String? = null,
        limit: Int = 50
    ): List<String>
}
