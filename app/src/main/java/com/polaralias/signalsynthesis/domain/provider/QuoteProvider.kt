package com.polaralias.signalsynthesis.domain.provider

import com.polaralias.signalsynthesis.domain.model.Quote

interface QuoteProvider {
    suspend fun getQuotes(symbols: List<String>): Map<String, Quote>
}
