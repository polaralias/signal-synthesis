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
            useMockDataWhenOffline = prefs.getBoolean(KEY_MOCK_DATA, true),
            screenerConservativeThreshold = prefs.getFloat(KEY_SCREEN_CONS, 5.0f).toDouble(),
            screenerModerateThreshold = prefs.getFloat(KEY_SCREEN_MOD, 20.0f).toDouble(),
            screenerAggressiveThreshold = prefs.getFloat(KEY_SCREEN_AGGR, 100.0f).toDouble(),
            screenerMinVolume = prefs.getLong(KEY_SCREEN_VOL, 1_000_000L),
            llmProvider = com.polaralias.signalsynthesis.domain.ai.LlmProvider.valueOf(prefs.getString(KEY_LLM_PROVIDER, "OPENAI") ?: "OPENAI"),
            analysisModel = com.polaralias.signalsynthesis.domain.ai.LlmModel.valueOf(prefs.getString(KEY_ANALYSIS_MODEL, "GPT_5_1") ?: "GPT_5_1"),
            verdictModel = com.polaralias.signalsynthesis.domain.ai.LlmModel.valueOf(prefs.getString(KEY_VERDICT_MODEL, "GPT_5_1") ?: "GPT_5_1"),
            reasoningModel = com.polaralias.signalsynthesis.domain.ai.LlmModel.valueOf(prefs.getString(KEY_REASONING_MODEL, "GPT_5_2") ?: "GPT_5_2"),
            reasoningDepth = com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.valueOf(prefs.getString(KEY_REASONING_DEPTH, "MEDIUM") ?: "MEDIUM"),
            outputLength = com.polaralias.signalsynthesis.domain.ai.OutputLength.valueOf(prefs.getString(KEY_OUTPUT_LENGTH, "STANDARD") ?: "STANDARD"),
            verbosity = com.polaralias.signalsynthesis.domain.ai.Verbosity.valueOf(prefs.getString(KEY_VERBOSITY, "MEDIUM") ?: "MEDIUM"),
            riskTolerance = com.polaralias.signalsynthesis.data.settings.RiskTolerance.valueOf(prefs.getString(KEY_RISK_TOLERANCE, "MODERATE") ?: "MODERATE"),
            preferredAssetClass = com.polaralias.signalsynthesis.data.settings.AssetClass.valueOf(prefs.getString(KEY_ASSET_CLASS, "STOCKS") ?: "STOCKS"),
            discoveryMode = com.polaralias.signalsynthesis.data.settings.DiscoveryMode.valueOf(prefs.getString(KEY_DISCOVERY_MODE, "CURATED") ?: "CURATED"),
            isAnalysisPaused = prefs.getBoolean(KEY_ANALYSIS_PAUSED, false),
            useStagedPipeline = prefs.getBoolean(KEY_USE_STAGED, false),
            themeMode = com.polaralias.signalsynthesis.data.settings.ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"),
            modelRouting = com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig.fromJson(prefs.getString(KEY_MODEL_ROUTING, null)),
            rssFeeds = com.squareup.moshi.Moshi.Builder().build().adapter<List<String>>(List::class.java).fromJson(prefs.getString(KEY_RSS_FEEDS, "[]") ?: "[]") ?: emptyList()
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
            putFloat(KEY_SCREEN_CONS, settings.screenerConservativeThreshold.toFloat())
            putFloat(KEY_SCREEN_MOD, settings.screenerModerateThreshold.toFloat())
            putFloat(KEY_SCREEN_AGGR, settings.screenerAggressiveThreshold.toFloat())
            putLong(KEY_SCREEN_VOL, settings.screenerMinVolume)
            putString(KEY_LLM_PROVIDER, settings.llmProvider.name)
            putString(KEY_ANALYSIS_MODEL, settings.analysisModel.name)
            putString(KEY_VERDICT_MODEL, settings.verdictModel.name)
            putString(KEY_REASONING_MODEL, settings.reasoningModel.name)
            putString(KEY_REASONING_DEPTH, settings.reasoningDepth.name)
            putString(KEY_OUTPUT_LENGTH, settings.outputLength.name)
            putString(KEY_VERBOSITY, settings.verbosity.name)
            putString(KEY_RISK_TOLERANCE, settings.riskTolerance.name)
            putString(KEY_ASSET_CLASS, settings.preferredAssetClass.name)
            putString(KEY_DISCOVERY_MODE, settings.discoveryMode.name)
            putBoolean(KEY_ANALYSIS_PAUSED, settings.isAnalysisPaused)
            putBoolean(KEY_USE_STAGED, settings.useStagedPipeline)
            putString(KEY_THEME_MODE, settings.themeMode.name)
            putString(KEY_MODEL_ROUTING, settings.modelRouting.toJson())
            putString(KEY_RSS_FEEDS, com.squareup.moshi.Moshi.Builder().build().adapter<List<String>>(List::class.java).toJson(settings.rssFeeds))
        }
    }

    suspend fun loadCustomTickers(): List<String> = withContext(Dispatchers.IO) {
        prefs.getString(KEY_CUSTOM_TICKERS, "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun saveCustomTickers(tickers: List<String>) = withContext(Dispatchers.IO) {
        prefs.edit {
            putString(KEY_CUSTOM_TICKERS, tickers.joinToString(","))
        }
    }

    suspend fun loadBlocklist(): List<String> = withContext(Dispatchers.IO) {
        prefs.getString(KEY_BLOCKLIST, "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun saveBlocklist(tickers: List<String>) = withContext(Dispatchers.IO) {
        prefs.edit {
            putString(KEY_BLOCKLIST, tickers.joinToString(","))
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
        private const val KEY_SCREEN_CONS = "screen_cons"
        private const val KEY_SCREEN_MOD = "screen_mod"
        private const val KEY_SCREEN_AGGR = "screen_aggr"
        private const val KEY_SCREEN_VOL = "screen_vol"
        private const val KEY_CUSTOM_TICKERS = "custom_tickers_list"
        private const val KEY_BLOCKLIST = "blocklist_tickers"
        private const val KEY_LLM_PROVIDER = "llm_provider"
        private const val KEY_ANALYSIS_MODEL = "analysis_model"
        private const val KEY_VERDICT_MODEL = "verdict_model"
        private const val KEY_REASONING_MODEL = "reasoning_model"
        private const val KEY_REASONING_DEPTH = "reasoning_depth"
        private const val KEY_OUTPUT_LENGTH = "output_length"
        private const val KEY_VERBOSITY = "verbosity"
        private const val KEY_RISK_TOLERANCE = "risk_tolerance_profile"
        private const val KEY_ASSET_CLASS = "preferred_asset_class"
        private const val KEY_DISCOVERY_MODE = "discovery_mode"
        private const val KEY_ANALYSIS_PAUSED = "analysis_paused"
        private const val KEY_USE_STAGED = "use_staged_pipeline"
        private const val KEY_THEME_MODE = "interface_theme_mode"
        private const val KEY_MODEL_ROUTING = "model_routing_by_stage"
        private const val KEY_RSS_FEEDS = "user_rss_feeds"
    }
}
