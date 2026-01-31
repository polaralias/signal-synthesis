package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.domain.model.TradingIntent

/**
 * Discovers candidate symbols for analysis based on trading intent.
 * 
 * In the initial implementation, this returns a curated list of high-liquidity symbols.
 * Future versions may integrate with a screener API or dynamic discovery logic.
 */
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository

class DiscoverCandidatesUseCase(
    private val repository: MarketDataRepository
) {
    
    /**
     * Discover candidate symbols based on trading intent and risk tolerance.
     * 
     * @param intent The trading intent (DAY_TRADE, SWING, LONG_TERM)
     * @param risk The user's risk tolerance
     * @param customTickers User-supplied list of tickers to include
     * @return List of symbol candidates
     */
    suspend fun execute(
        intent: com.polaralias.signalsynthesis.domain.model.TradingIntent,
        risk: com.polaralias.signalsynthesis.data.settings.RiskTolerance = com.polaralias.signalsynthesis.data.settings.RiskTolerance.MODERATE,
        assetClass: com.polaralias.signalsynthesis.data.settings.AssetClass = com.polaralias.signalsynthesis.data.settings.AssetClass.STOCKS,
        discoveryMode: com.polaralias.signalsynthesis.data.settings.DiscoveryMode = com.polaralias.signalsynthesis.data.settings.DiscoveryMode.STATIC,
        customTickers: List<String> = emptyList(),
        screenerThresholds: Map<String, Double> = emptyMap()
    ): Map<String, com.polaralias.signalsynthesis.domain.model.TickerSource> {
        val candidates = mutableMapOf<String, com.polaralias.signalsynthesis.domain.model.TickerSource>()

        // Add custom tickers first (highest priority)
        customTickers.forEach { candidates[it] = com.polaralias.signalsynthesis.domain.model.TickerSource.CUSTOM }

        if (discoveryMode != com.polaralias.signalsynthesis.data.settings.DiscoveryMode.CUSTOM) {
            if (assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.FOREX || assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.ALL) {
                forexCandidates().forEach { candidates.putIfAbsent(it, com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED) }
            }
            
            if (assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.METALS || assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.ALL) {
                metalsCandidates().forEach { candidates.putIfAbsent(it, com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED) }
            }
        }

        if (assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.STOCKS || assetClass == com.polaralias.signalsynthesis.data.settings.AssetClass.ALL) {
            
            if (discoveryMode == com.polaralias.signalsynthesis.data.settings.DiscoveryMode.STATIC) {
                // Use curated lists
                val baseList = when (intent) {
                    com.polaralias.signalsynthesis.domain.model.TradingIntent.DAY_TRADE -> dayTradeCandidates()
                    com.polaralias.signalsynthesis.domain.model.TradingIntent.SWING -> swingTradeCandidates()
                    com.polaralias.signalsynthesis.domain.model.TradingIntent.LONG_TERM -> longTermCandidates()
                }

                val riskFiltered = when (risk) {
                    com.polaralias.signalsynthesis.data.settings.RiskTolerance.CONSERVATIVE -> {
                        baseList.filterNot { it in listOf("TSLA", "AMD", "NVDA", "NFLX") }
                    }
                    com.polaralias.signalsynthesis.data.settings.RiskTolerance.MODERATE -> baseList
                    com.polaralias.signalsynthesis.data.settings.RiskTolerance.AGGRESSIVE -> {
                        baseList + listOf("RIOT", "MARA", "PLTR", "SOFI", "AMC", "GME")
                    }
                }
                
                riskFiltered.forEach { candidates.putIfAbsent(it, com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED) }
            }

            if (discoveryMode == com.polaralias.signalsynthesis.data.settings.DiscoveryMode.SCREENER) {
                // Fetch from Screener (replaces static list in this mode)
                val (minVol, minPrice) = when (risk) {
                    com.polaralias.signalsynthesis.data.settings.RiskTolerance.CONSERVATIVE -> 2_000_000L to (screenerThresholds["conservative"] ?: 20.0)
                    com.polaralias.signalsynthesis.data.settings.RiskTolerance.MODERATE -> 1_000_000L to (screenerThresholds["moderate"] ?: 10.0)
                    com.polaralias.signalsynthesis.data.settings.RiskTolerance.AGGRESSIVE -> 500_000L to (screenerThresholds["aggressive"] ?: 2.0)
                }
                
                val screened = try {
                    repository.screenStocks(
                        minPrice = minPrice, 
                        maxPrice = null,
                        minVolume = minVol, 
                        sector = null,
                        limit = 20
                    )
                } catch (e: Exception) {
                    com.polaralias.signalsynthesis.util.Logger.e("DiscoverCandidates", "Screener fetch failed", e)
                    emptyList()
                }
                
                screened.forEach { candidates.putIfAbsent(it, com.polaralias.signalsynthesis.domain.model.TickerSource.SCREENER) }
            }
        }

        return candidates
    }

    private fun forexCandidates(): List<String> {
        return listOf(
            "EUR/USD", "GBP/USD", "USD/JPY", "USD/CAD", "AUD/USD",
            "EUR/GBP", "EUR/JPY", "GBP/JPY", "NZD/USD", "USD/CHF"
        )
    }

    private fun metalsCandidates(): List<String> {
        return listOf(
            "XAU/USD", "XAG/USD", "PA/USD", "PL/USD", // Spot Metals
            "GOLD", "SILVER", "PLATINUM", "PALLADIUM", // Alternate names for some providers
            "GLD", "SLV", "IAU", "PPLT" // Liquid ETFs
        )
    }
    
    /**
     * Candidates suitable for day trading: high volume, high volatility.
     */
    private fun dayTradeCandidates(): List<String> {
        return listOf(
            // Tech giants with high volume
            "AAPL", "MSFT", "GOOGL", "AMZN", "META", "NVDA", "TSLA",
            // Financial sector
            "JPM", "BAC", "GS", "MS",
            // High-volume ETFs
            "SPY", "QQQ", "IWM",
            // Other active stocks
            "AMD", "NFLX", "DIS", "BA", "INTC"
        )
    }
    
    /**
     * Candidates suitable for swing trading: good fundamentals, moderate volatility.
     */
    private fun swingTradeCandidates(): List<String> {
        return listOf(
            // Major tech
            "AAPL", "MSFT", "GOOGL", "AMZN", "META", "NVDA",
            // Financial
            "JPM", "BAC", "GS", "V", "MA",
            // Healthcare
            "JNJ", "UNH", "PFE", "ABBV",
            // Consumer
            "WMT", "HD", "MCD", "NKE", "COST",
            // Industrial
            "CAT", "BA", "GE",
            // Energy
            "XOM", "CVX"
        )
    }
    
    /**
     * Candidates suitable for long-term investing: blue chips, stable growth.
     */
    private fun longTermCandidates(): List<String> {
        return listOf(
            // Blue chip tech
            "AAPL", "MSFT", "GOOGL", "AMZN",
            // Established financials
            "JPM", "BAC", "V", "MA", "BRK.B",
            // Healthcare leaders
            "JNJ", "UNH", "ABBV", "LLY",
            // Consumer staples
            "PG", "KO", "PEP", "WMT", "COST",
            // Industrials
            "CAT", "HON", "UPS",
            // Energy majors
            "XOM", "CVX",
            // Diversified ETFs
            "SPY", "VOO", "VTI"
        )
    }
}
