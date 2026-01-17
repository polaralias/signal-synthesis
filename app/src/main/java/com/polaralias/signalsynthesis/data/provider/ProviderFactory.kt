package com.polaralias.signalsynthesis.data.provider

import com.polaralias.signalsynthesis.data.provider.alpaca.AlpacaMarketDataProvider
import com.polaralias.signalsynthesis.data.provider.finnhub.FinnhubMarketDataProvider
import com.polaralias.signalsynthesis.data.provider.fmp.FmpMarketDataProvider
import com.polaralias.signalsynthesis.data.provider.polygon.PolygonMarketDataProvider

interface MarketDataProviderFactory {
    fun build(keys: ApiKeys): ProviderBundle
}

class ProviderFactory(
    private val includeMock: Boolean = true
) : MarketDataProviderFactory {
    override fun build(keys: ApiKeys): ProviderBundle {
        val hasKeys = keys.hasAny()
        
        // Create providers based on available keys
        // Priority: Alpaca > Polygon > Finnhub > FMP
        val alpacaProvider = if (keys.hasAlpaca()) {
            AlpacaMarketDataProvider(keys.alpacaKey!!, keys.alpacaSecret!!)
        } else null
        
        val polygonProvider = keys.polygon?.takeIf { it.isNotBlank() }?.let {
            PolygonMarketDataProvider(it)
        }
        
        val finnhubProvider = keys.finnhub?.takeIf { it.isNotBlank() }?.let {
            FinnhubMarketDataProvider(it)
        }
        
        val fmpProvider = keys.financialModelingPrep?.takeIf { it.isNotBlank() }?.let {
            FmpMarketDataProvider(it)
        }
        
        val mockProvider = if (includeMock || !hasKeys) MockMarketDataProvider() else null

        // Build provider lists with fallback ordering
        // For quotes and intraday: prefer Alpaca/Polygon (more real-time)
        val quoteProviders = listOfNotNull(alpacaProvider, polygonProvider, finnhubProvider, fmpProvider, mockProvider)
        val intradayProviders = listOfNotNull(alpacaProvider, polygonProvider, finnhubProvider, fmpProvider, mockProvider)
        val dailyProviders = listOfNotNull(alpacaProvider, polygonProvider, finnhubProvider, fmpProvider, mockProvider)
        
        // For profiles: prefer FMP/Finnhub/Polygon (better fundamental data)
        val profileProviders = listOfNotNull(fmpProvider, finnhubProvider, polygonProvider, alpacaProvider, mockProvider)
        
        // For metrics: prefer FMP/Finnhub (only ones with PE, EPS)
        val metricsProviders = listOfNotNull(fmpProvider, finnhubProvider, polygonProvider, mockProvider)
        
        // For sentiment: prefer FMP/Finnhub (only ones with sentiment)
        val sentimentProviders = listOfNotNull(fmpProvider, finnhubProvider, mockProvider)

        return ProviderBundle(
            quoteProviders = quoteProviders,
            intradayProviders = intradayProviders,
            dailyProviders = dailyProviders,
            profileProviders = profileProviders,
            metricsProviders = metricsProviders,
            sentimentProviders = sentimentProviders
        )
    }
}
