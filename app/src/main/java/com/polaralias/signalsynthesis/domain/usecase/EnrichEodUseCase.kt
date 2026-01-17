package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.indicators.SmaIndicator
import com.polaralias.signalsynthesis.domain.model.EodStats

/**
 * Enriches symbols with end-of-day statistics: SMA-50, SMA-200.
 */
class EnrichEodUseCase(
    private val repository: MarketDataRepository
) {
    /**
     * Enrich symbols with end-of-day statistics.
     * 
     * @param symbols List of symbols to enrich
     * @param days Number of days of daily data to fetch (default 200, minimum for SMA-200)
     * @return Map of symbol to EodStats
     */
    suspend fun execute(symbols: List<String>, days: Int = 200): Map<String, EodStats> {
        if (symbols.isEmpty()) return emptyMap()
        
        val results = mutableMapOf<String, EodStats>()
        
        for (symbol in symbols) {
            try {
                val bars = repository.getDaily(symbol, days)
                if (bars.isEmpty()) continue
                
                // Calculate SMAs
                val sma50 = SmaIndicator.calculateFromDaily(bars, period = 50)
                val sma200 = SmaIndicator.calculateFromDaily(bars, period = 200)
                
                results[symbol] = EodStats(
                    sma50 = sma50,
                    sma200 = sma200
                )
            } catch (e: Exception) {
                // Log error and continue with other symbols
                continue
            }
        }
        
        return results
    }
}
