package com.signalsynthesis.domain.provider

import com.signalsynthesis.domain.model.Quote

interface QuoteProvider {
    suspend fun getQuotes(symbols: List<String>): Map<String, Quote>
}
