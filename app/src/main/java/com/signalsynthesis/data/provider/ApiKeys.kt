package com.signalsynthesis.data.provider

data class ApiKeys(
    val alpaca: String? = null,
    val polygon: String? = null,
    val finnhub: String? = null,
    val financialModelingPrep: String? = null
) {
    fun hasAny(): Boolean {
        return listOf(alpaca, polygon, finnhub, financialModelingPrep)
            .any { !it.isNullOrBlank() }
    }
}
