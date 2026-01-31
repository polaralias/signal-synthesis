package com.polaralias.signalsynthesis.data.storage

import android.content.Context
import com.polaralias.signalsynthesis.data.alerts.AlertSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

interface AlertSettingsStorage {
    suspend fun loadSettings(): AlertSettings
    suspend fun saveSettings(settings: AlertSettings)
    suspend fun loadSymbols(): List<String>
    suspend fun saveSymbols(symbols: List<String>)
    suspend fun loadTargets(): List<com.polaralias.signalsynthesis.data.alerts.AlertTarget>
    suspend fun saveTargets(targets: List<com.polaralias.signalsynthesis.data.alerts.AlertTarget>)
    suspend fun getLastAlertTimestamp(symbol: String, type: com.polaralias.signalsynthesis.data.alerts.AlertType): Long
    suspend fun setLastAlertTimestamp(symbol: String, type: com.polaralias.signalsynthesis.data.alerts.AlertType, timestamp: Long)
}

class AlertSettingsStore(context: Context) : AlertSettingsStorage {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private val targetsAdapter = moshi.adapter<List<com.polaralias.signalsynthesis.data.alerts.AlertTarget>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, com.polaralias.signalsynthesis.data.alerts.AlertTarget::class.java)
    )

    override suspend fun loadSettings(): AlertSettings = withContext(Dispatchers.IO) {
        AlertSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            vwapDipPercent = preferences.getFloat(KEY_VWAP_DIP, AlertSettings.DEFAULT_VWAP_DIP_PERCENT.toFloat()).toDouble(),
            rsiOversold = preferences.getFloat(KEY_RSI_OVERSOLD, AlertSettings.DEFAULT_RSI_OVERSOLD.toFloat()).toDouble(),
            rsiOverbought = preferences.getFloat(KEY_RSI_OVERBOUGHT, AlertSettings.DEFAULT_RSI_OVERBOUGHT.toFloat()).toDouble(),
            cooldownMinutes = preferences.getInt(KEY_COOLDOWN_MINUTES, AlertSettings.DEFAULT_COOLDOWN_MINUTES)
        )
    }

    override suspend fun saveSettings(settings: AlertSettings) = withContext(Dispatchers.IO) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putFloat(KEY_VWAP_DIP, settings.vwapDipPercent.toFloat())
            .putFloat(KEY_RSI_OVERSOLD, settings.rsiOversold.toFloat())
            .putFloat(KEY_RSI_OVERBOUGHT, settings.rsiOverbought.toFloat())
            .putInt(KEY_COOLDOWN_MINUTES, settings.cooldownMinutes)
            .apply()
    }

    override suspend fun loadSymbols(): List<String> = withContext(Dispatchers.IO) {
        preferences.getString(KEY_SYMBOLS, "")
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    override suspend fun saveSymbols(symbols: List<String>) = withContext(Dispatchers.IO) {
        val normalized = symbols
            .map { it.trim().uppercase(Locale.US) }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(",")
        preferences.edit().putString(KEY_SYMBOLS, normalized).apply()
    }

    override suspend fun loadTargets(): List<com.polaralias.signalsynthesis.data.alerts.AlertTarget> = withContext(Dispatchers.IO) {
        val raw = preferences.getString(KEY_TARGETS, null) ?: return@withContext emptyList()
        targetsAdapter.fromJson(raw) ?: emptyList()
    }

    override suspend fun saveTargets(targets: List<com.polaralias.signalsynthesis.data.alerts.AlertTarget>) = withContext(Dispatchers.IO) {
        val normalized = targets
            .groupBy { it.symbol.trim().uppercase(Locale.US) }
            .mapNotNull { (_, list) -> list.firstOrNull() }
        preferences.edit().putString(KEY_TARGETS, targetsAdapter.toJson(normalized)).apply()
    }

    override suspend fun getLastAlertTimestamp(symbol: String, type: com.polaralias.signalsynthesis.data.alerts.AlertType): Long = withContext(Dispatchers.IO) {
        preferences.getLong(alertKey(symbol, type), 0L)
    }

    override suspend fun setLastAlertTimestamp(symbol: String, type: com.polaralias.signalsynthesis.data.alerts.AlertType, timestamp: Long) = withContext(Dispatchers.IO) {
        preferences.edit().putLong(alertKey(symbol, type), timestamp).apply()
    }

    private fun alertKey(symbol: String, type: com.polaralias.signalsynthesis.data.alerts.AlertType): String {
        return "${KEY_ALERT_PREFIX}${symbol.trim().uppercase(Locale.US)}_${type.name}"
    }

    companion object {
        private const val PREFS_NAME = "signal_synthesis_alerts"
        private const val KEY_ENABLED = "alerts_enabled"
        private const val KEY_SYMBOLS = "alerts_symbols"
        private const val KEY_VWAP_DIP = "alerts_vwap_dip"
        private const val KEY_RSI_OVERSOLD = "alerts_rsi_oversold"
        private const val KEY_RSI_OVERBOUGHT = "alerts_rsi_overbought"
        private const val KEY_COOLDOWN_MINUTES = "alerts_cooldown_minutes"
        private const val KEY_TARGETS = "alerts_price_targets"
        private const val KEY_ALERT_PREFIX = "alerts_last_"
    }
}
