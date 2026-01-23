package com.polaralias.signalsynthesis.data.provider

import com.polaralias.signalsynthesis.data.provider.alpaca.AlpacaMarketDataProvider
import com.polaralias.signalsynthesis.data.provider.finnhub.FinnhubMarketDataProvider
import com.polaralias.signalsynthesis.data.provider.fmp.FmpMarketDataProvider
import com.polaralias.signalsynthesis.data.provider.polygon.MassiveMarketDataProvider

interface MarketDataProviderFactory {
    fun build(keys: ApiKeys): ProviderBundle
}

class ProviderFactory(
    private val includeMock: Boolean = true
) : MarketDataProviderFactory {
    override fun build(keys: ApiKeys): ProviderBundle {
        val hasKeys = keys.hasAny()
        
        // Create providers based on available keys
        // Priority: Alpaca > Massive > Finnhub > FMP
        val alpacaProvider = if (keys.hasAlpaca()) {
            AlpacaMarketDataProvider(keys.alpacaKey!!, keys.alpacaSecret!!)
        } else null
        
        val massiveProvider = keys.massive?.takeIf { it.isNotBlank() }?.let {
            MassiveMarketDataProvider(it)
        }
        
        val finnhubProvider = keys.finnhub?.takeIf { it.isNotBlank() }?.let {
            FinnhubMarketDataProvider(it)
        }
        
        val fmpProvider = keys.financialModelingPrep?.takeIf { it.isNotBlank() }?.let {
            FmpMarketDataProvider(it)
        }

        val twelveDataProvider = keys.twelveData?.takeIf { it.isNotBlank() }?.let {
            com.polaralias.signalsynthesis.data.provider.twelvedata.TwelveDataMarketDataProvider(it)
        }
        
        val mockProvider = if (!hasKeys) MockMarketDataProvider() else null

        // Build provider lists with fallback ordering
        // For quotes and intraday: prefer Alpaca/Massive (more real-time)
        val quoteProviders = listOfNotNull(alpacaProvider, massiveProvider, twelveDataProvider, finnhubProvider, fmpProvider, mockProvider)
        val intradayProviders = listOfNotNull(alpacaProvider, massiveProvider, twelveDataProvider, finnhubProvider, fmpProvider, mockProvider)
        val dailyProviders = listOfNotNull(alpacaProvider, massiveProvider, twelveDataProvider, finnhubProvider, fmpProvider, mockProvider)
        
        // For profiles: prefer FMP/Finnhub/Massive (better fundamental data)
        val profileProviders = listOfNotNull(fmpProvider, finnhubProvider, massiveProvider, twelveDataProvider, alpacaProvider, mockProvider)
        
        // For metrics: prefer FMP/Finnhub (only ones with PE, EPS)
        val metricsProviders = listOfNotNull(fmpProvider, finnhubProvider, massiveProvider, twelveDataProvider, mockProvider)
        
        // For sentiment: prefer FMP/Finnhub (only ones with sentiment)
        val sentimentProviders = listOfNotNull(fmpProvider, finnhubProvider, mockProvider)

        // For screener: prefer FMP (native screener) > Massive (tickers list) > Mock
        val screenerProviders = listOfNotNull(fmpProvider, massiveProvider, mockProvider)

        val searchProviders = listOfNotNull(fmpProvider, massiveProvider, finnhubProvider)

        return ProviderBundle(
            quoteProviders = quoteProviders,
            intradayProviders = intradayProviders,
            dailyProviders = dailyProviders,
            profileProviders = profileProviders,
            metricsProviders = metricsProviders,
            sentimentProviders = sentimentProviders,
            screenerProviders = screenerProviders,
            searchProviders = searchProviders
        )
    }
}
