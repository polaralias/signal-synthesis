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
     * Discover candidate symbols based on trading intent.
     * 
     * @param intent The trading intent (DAY_TRADE, SWING, LONG_TERM)
     * @return List of symbol candidates
     */
    fun execute(intent: TradingIntent): List<String> {
        // Start with a curated list of highly liquid stocks
        // These represent major market movers with good volume
        return when (intent) {
            TradingIntent.DAY_TRADE -> dayTradeCandidates()
            TradingIntent.SWING -> swingTradeCandidates()
            TradingIntent.LONG_TERM -> longTermCandidates()
        }
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
