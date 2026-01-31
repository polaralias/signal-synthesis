package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.indicators.AtrIndicator
import com.polaralias.signalsynthesis.domain.indicators.RsiIndicator
import com.polaralias.signalsynthesis.domain.indicators.VwapIndicator
import com.polaralias.signalsynthesis.domain.model.IntradayStats

/**
 * Enriches symbols with intraday statistics: VWAP, RSI-14, ATR-14.
 */
class EnrichIntradayUseCase(
    private val repository: MarketDataRepository
) {
    /**
     * Enrich symbols with intraday statistics.
     * 
     * @param symbols List of symbols to enrich
     * @param days Number of days of intraday data to fetch (default 2)
     * @return Map of symbol to IntradayStats
     */
    suspend fun execute(symbols: List<String>, days: Int = 2): Map<String, IntradayStats> {
        if (symbols.isEmpty()) return emptyMap()
        
        val results = mutableMapOf<String, IntradayStats>()
        
        for (symbol in symbols) {
            kotlinx.coroutines.yield()
            try {
                val bars = repository.getIntraday(symbol, days)
                if (bars.isEmpty()) continue
                
                // Calculate indicators
                val vwap = VwapIndicator.calculate(bars)
                val rsi14 = RsiIndicator.calculateFromIntraday(bars, period = 14)
                val atr14 = AtrIndicator.calculateFromIntraday(bars, period = 14)
                
                results[symbol] = IntradayStats(
                    vwap = vwap,
                    rsi14 = rsi14,
                    atr14 = atr14
                )
            } catch (e: Exception) {
                com.polaralias.signalsynthesis.util.Logger.e("EnrichIntraday", "Intraday enrichment failed for $symbol", e)
                continue
            }
        }
        
        return results
    }
}
