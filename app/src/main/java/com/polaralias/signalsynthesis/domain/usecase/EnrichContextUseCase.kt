package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.SentimentData

/**
 * Data class to hold context data for a symbol.
 */
data class SymbolContext(
    val profile: CompanyProfile?,
    val metrics: FinancialMetrics?,
    val sentiment: SentimentData?
)

/**
 * Enriches symbols with context data: profile, metrics, sentiment.
 */
class EnrichContextUseCase(
    private val repository: MarketDataRepository
) {
    /**
     * Enrich symbols with context data.
     * 
     * @param symbols List of symbols to enrich
     * @return Map of symbol to SymbolContext
     */
    suspend fun execute(symbols: List<String>): Map<String, SymbolContext> {
        if (symbols.isEmpty()) return emptyMap()
        
        val results = mutableMapOf<String, SymbolContext>()
        
        for (symbol in symbols) {
            kotlinx.coroutines.yield()
            try {
                val profile = try {
                    repository.getProfile(symbol)
                } catch (e: Exception) {
                    com.polaralias.signalsynthesis.util.Logger.e("EnrichContext", "Profile fetch failed for $symbol", e)
                    null
                }
                
                val metrics = try {
                    repository.getMetrics(symbol)
                } catch (e: Exception) {
                    com.polaralias.signalsynthesis.util.Logger.e("EnrichContext", "Metrics fetch failed for $symbol", e)
                    null
                }
                
                val sentiment = try {
                    repository.getSentiment(symbol)
                } catch (e: Exception) {
                    com.polaralias.signalsynthesis.util.Logger.e("EnrichContext", "Sentiment fetch failed for $symbol", e)
                    null
                }
                
                results[symbol] = SymbolContext(
                    profile = profile,
                    metrics = metrics,
                    sentiment = sentiment
                )
            } catch (e: Exception) {
                com.polaralias.signalsynthesis.util.Logger.e("EnrichContext", "Context enrichment failed for $symbol", e)
                continue
            }
        }
        
        return results
    }
}
