package com.polaralias.signalsynthesis.data.storage

import android.content.Context
import com.polaralias.signalsynthesis.data.alerts.AlertSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AlertSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun loadSettings(): AlertSettings = withContext(Dispatchers.IO) {
        AlertSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            vwapDipPercent = preferences.getFloat(KEY_VWAP_DIP, AlertSettings.DEFAULT_VWAP_DIP_PERCENT.toFloat()).toDouble(),
            rsiOversold = preferences.getFloat(KEY_RSI_OVERSOLD, AlertSettings.DEFAULT_RSI_OVERSOLD.toFloat()).toDouble(),
            rsiOverbought = preferences.getFloat(KEY_RSI_OVERBOUGHT, AlertSettings.DEFAULT_RSI_OVERBOUGHT.toFloat()).toDouble()
        )
    }

    suspend fun saveSettings(settings: AlertSettings) = withContext(Dispatchers.IO) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putFloat(KEY_VWAP_DIP, settings.vwapDipPercent.toFloat())
            .putFloat(KEY_RSI_OVERSOLD, settings.rsiOversold.toFloat())
            .putFloat(KEY_RSI_OVERBOUGHT, settings.rsiOverbought.toFloat())
            .apply()
    }

    suspend fun loadSymbols(): List<String> = withContext(Dispatchers.IO) {
        preferences.getString(KEY_SYMBOLS, "")
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    suspend fun saveSymbols(symbols: List<String>) = withContext(Dispatchers.IO) {
        val normalized = symbols
            .map { it.trim().uppercase(Locale.US) }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(",")
        preferences.edit().putString(KEY_SYMBOLS, normalized).apply()
    }

    companion object {
        private const val PREFS_NAME = "signal_synthesis_alerts"
        private const val KEY_ENABLED = "alerts_enabled"
        private const val KEY_SYMBOLS = "alerts_symbols"
        private const val KEY_VWAP_DIP = "alerts_vwap_dip"
        private const val KEY_RSI_OVERSOLD = "alerts_rsi_oversold"
        private const val KEY_RSI_OVERBOUGHT = "alerts_rsi_overbought"
    }
}
