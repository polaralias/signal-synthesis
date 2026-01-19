package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.domain.model.TradingIntent

/**
 * Discovers candidate symbols for analysis based on trading intent.
 * 
 * In the initial implementation, this returns a curated list of high-liquidity symbols.
 * Future versions may integrate with a screener API or dynamic discovery logic.
 */
class DiscoverCandidatesUseCase {
    
    /**
     * Discover candidate symbols based on trading intent and risk tolerance.
     * 
     * @param intent The trading intent (DAY_TRADE, SWING, LONG_TERM)
     * @param risk The user's risk tolerance
     * @return List of symbol candidates
     */
    fun execute(
        intent: com.polaralias.signalsynthesis.domain.model.TradingIntent,
        risk: com.polaralias.signalsynthesis.data.settings.RiskTolerance = com.polaralias.signalsynthesis.data.settings.RiskTolerance.MODERATE
    ): List<String> {
        val baseList = when (intent) {
            com.polaralias.signalsynthesis.domain.model.TradingIntent.DAY_TRADE -> dayTradeCandidates()
            com.polaralias.signalsynthesis.domain.model.TradingIntent.SWING -> swingTradeCandidates()
            com.polaralias.signalsynthesis.domain.model.TradingIntent.LONG_TERM -> longTermCandidates()
        }

        return when (risk) {
            com.polaralias.signalsynthesis.data.settings.RiskTolerance.CONSERVATIVE -> {
                // Filter out some of the more volatile names
                baseList.filterNot { it in listOf("TSLA", "AMD", "NVDA", "NFLX") }
            }
            com.polaralias.signalsynthesis.data.settings.RiskTolerance.MODERATE -> baseList
            com.polaralias.signalsynthesis.data.settings.RiskTolerance.AGGRESSIVE -> {
                // Add some speculative/small cap names
                baseList + listOf("RIOT", "MARA", "PLTR", "SOFI", "AMC", "GME")
            }
        }.distinct()
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
