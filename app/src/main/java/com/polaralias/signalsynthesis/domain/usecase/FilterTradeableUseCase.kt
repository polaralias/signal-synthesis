package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.repository.MarketDataRepository

/**
 * Filters symbols based on tradeability criteria:
 * - Price >= $1.00
 * - Volume > 0
 */
class FilterTradeableUseCase(
    private val repository: MarketDataRepository
) {
    /**
     * Filter symbols to keep only tradeable ones.
     * 
     * @param symbols List of symbols to filter
     * @param minPrice Minimum price threshold (default $1.00)
     * @return List of tradeable symbols
     */
    suspend fun execute(symbols: List<String>, minPrice: Double = 1.0): List<String> {
        if (symbols.isEmpty()) return emptyList()
        
        try {
            val quotes = repository.getQuotes(symbols)
            
            return symbols.filter { symbol ->
                val quote = quotes[symbol] ?: return@filter false
                quote.price >= minPrice && quote.volume > 0
            }
        } catch (e: Exception) {
            // If quotes fail, return empty list rather than unfiltered symbols
            return emptyList()
        }
    }
}
