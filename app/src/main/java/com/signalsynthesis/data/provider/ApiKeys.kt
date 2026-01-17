package com.signalsynthesis.data.provider

data class ApiKeys(
    val alpacaKey: String? = null,
    val alpacaSecret: String? = null,
    val polygon: String? = null,
    val finnhub: String? = null,
    val financialModelingPrep: String? = null
) {
    fun hasAny(): Boolean {
        return listOf(polygon, finnhub, financialModelingPrep)
            .any { !it.isNullOrBlank() } ||
                (!alpacaKey.isNullOrBlank() && !alpacaSecret.isNullOrBlank())
    }
    
    fun hasAlpaca(): Boolean = !alpacaKey.isNullOrBlank() && !alpacaSecret.isNullOrBlank()
}
