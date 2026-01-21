package com.polaralias.signalsynthesis.domain.provider

interface ScreenerProvider {
    suspend fun screenStocks(
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minVolume: Long? = null,
        sector: String? = null,
        limit: Int = 50
    ): List<String>

    suspend fun getTopGainers(limit: Int = 10): List<String>
    suspend fun getTopLosers(limit: Int = 10): List<String>
    suspend fun getMostActive(limit: Int = 10): List<String>
}
