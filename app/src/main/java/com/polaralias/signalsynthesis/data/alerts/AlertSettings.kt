package com.polaralias.signalsynthesis.data.alerts

data class AlertSettings(
    val enabled: Boolean = false,
    val vwapDipPercent: Double = DEFAULT_VWAP_DIP_PERCENT,
    val rsiOversold: Double = DEFAULT_RSI_OVERSOLD,
    val rsiOverbought: Double = DEFAULT_RSI_OVERBOUGHT
) {
    companion object {
        const val DEFAULT_VWAP_DIP_PERCENT = 1.0
        const val DEFAULT_RSI_OVERSOLD = 30.0
        const val DEFAULT_RSI_OVERBOUGHT = 70.0
    }
}
