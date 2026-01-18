package com.polaralias.signalsynthesis.data.storage

import android.content.Context
import androidx.core.content.edit
import com.polaralias.signalsynthesis.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AppSettingsStorage {
    suspend fun loadSettings(): AppSettings
    suspend fun saveSettings(settings: AppSettings)
}

class AppSettingsStore(context: Context) : AppSettingsStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun loadSettings(): AppSettings = withContext(Dispatchers.IO) {
        AppSettings(
            quoteRefreshIntervalMinutes = prefs.getInt(KEY_QUOTE_REFRESH, 5),
            alertCheckIntervalMinutes = prefs.getInt(KEY_ALERT_INTERVAL, 15),
            vwapDipPercent = prefs.getFloat(KEY_VWAP_DIP, 1.0f).toDouble(),
            rsiOversold = prefs.getFloat(KEY_RSI_OVERSOLD, 30.0f).toDouble(),
            rsiOverbought = prefs.getFloat(KEY_RSI_OVERBOUGHT, 70.0f).toDouble(),
            useMockDataWhenOffline = prefs.getBoolean(KEY_MOCK_DATA, true)
        )
    }

    override suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        prefs.edit {
            putInt(KEY_QUOTE_REFRESH, settings.quoteRefreshIntervalMinutes)
            putInt(KEY_ALERT_INTERVAL, settings.alertCheckIntervalMinutes)
            putFloat(KEY_VWAP_DIP, settings.vwapDipPercent.toFloat())
            putFloat(KEY_RSI_OVERSOLD, settings.rsiOversold.toFloat())
            putFloat(KEY_RSI_OVERBOUGHT, settings.rsiOverbought.toFloat())
            putBoolean(KEY_MOCK_DATA, settings.useMockDataWhenOffline)
        }
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_QUOTE_REFRESH = "quote_refresh_interval"
        private const val KEY_ALERT_INTERVAL = "alert_check_interval"
        private const val KEY_VWAP_DIP = "vwap_dip"
        private const val KEY_RSI_OVERSOLD = "rsi_oversold"
        private const val KEY_RSI_OVERBOUGHT = "rsi_overbought"
        private const val KEY_MOCK_DATA = "use_mock_data"
    }
}
