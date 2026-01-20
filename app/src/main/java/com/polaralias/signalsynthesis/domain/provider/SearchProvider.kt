package com.polaralias.signalsynthesis.domain.provider

interface SearchProvider {
    suspend fun searchSymbols(query: String, limit: Int = 10): List<SearchResult>
}

data class SearchResult(
    val symbol: String,
    val name: String,
    val exchange: String? = null
)
