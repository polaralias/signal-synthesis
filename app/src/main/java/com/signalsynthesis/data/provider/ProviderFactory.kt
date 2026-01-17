package com.signalsynthesis.data.provider

import com.signalsynthesis.data.provider.finnhub.FinnhubMarketDataProvider

class ProviderFactory(
    private val includeMock: Boolean = true
) {
    fun build(keys: ApiKeys): ProviderBundle {
        val hasKeys = keys.hasAny()
        val finnhubProvider = keys.finnhub?.takeIf { it.isNotBlank() }?.let {
            FinnhubMarketDataProvider(it)
        }
        val mockProvider = if (includeMock || !hasKeys) MockMarketDataProvider() else null

        return ProviderBundle(
            quoteProviders = listOfNotNull(finnhubProvider, mockProvider),
            intradayProviders = listOfNotNull(finnhubProvider, mockProvider),
            dailyProviders = listOfNotNull(finnhubProvider, mockProvider),
            profileProviders = listOfNotNull(finnhubProvider, mockProvider),
            metricsProviders = listOfNotNull(finnhubProvider, mockProvider),
            sentimentProviders = listOfNotNull(finnhubProvider, mockProvider)
        )
    }
}
