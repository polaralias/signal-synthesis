package com.polaralias.signalsynthesis.data.alerts

data class AlertSettings(
    val enabled: Boolean = false,
    val vwapDipPercent: Double = DEFAULT_VWAP_DIP_PERCENT,
    val rsiOversold: Double = DEFAULT_RSI_OVERSOLD,
    val rsiOverbought: Double = DEFAULT_RSI_OVERBOUGHT,
    val cooldownMinutes: Int = DEFAULT_COOLDOWN_MINUTES
) {
    companion object {
        const val DEFAULT_VWAP_DIP_PERCENT = 1.0
        const val DEFAULT_RSI_OVERSOLD = 30.0
        const val DEFAULT_RSI_OVERBOUGHT = 70.0
        const val DEFAULT_COOLDOWN_MINUTES = 30
    }
}

enum class AlertType {
    VWAP_DIP,
    RSI_OVERSOLD,
    RSI_OVERBOUGHT,
    PRICE_TARGET
}

enum class AlertDirection {
    ABOVE,
    BELOW
}

data class AlertTarget(
    val symbol: String,
    val targetPrice: Double,
    val direction: AlertDirection
)
