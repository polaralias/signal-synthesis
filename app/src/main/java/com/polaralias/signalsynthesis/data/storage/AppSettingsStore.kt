package com.polaralias.signalsynthesis.data.storage

import android.content.Context
import androidx.core.content.edit
import com.polaralias.signalsynthesis.data.rss.RssFeedCatalogLoader
import com.polaralias.signalsynthesis.data.rss.RssFeedDefaults
import com.polaralias.signalsynthesis.data.settings.AppSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AppSettingsStorage {
    suspend fun loadSettings(): AppSettings
    suspend fun saveSettings(settings: AppSettings)
}

class AppSettingsStore(context: Context) : AppSettingsStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().build()
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )
    private val rssCatalog by lazy { RssFeedCatalogLoader(context).load() }

    override suspend fun loadSettings(): AppSettings = withContext(Dispatchers.IO) {
        val storedTopicsRaw = prefs.getString(KEY_RSS_ENABLED_TOPICS, null)
        val storedTopics = parseStringList(storedTopicsRaw).toSet()
        val storedTickerSourcesRaw = prefs.getString(KEY_RSS_TICKER_SOURCES, null)
        val storedTickerSources = parseStringList(storedTickerSourcesRaw).toSet()
        val hasStoredTopics = prefs.contains(KEY_RSS_ENABLED_TOPICS)
        val hasStoredTickerSources = prefs.contains(KEY_RSS_TICKER_SOURCES)
        val legacyTopics = if (!hasStoredTopics) migrateLegacyTopics() else emptySet()
        val enabledTopics = when {
            hasStoredTopics -> storedTopics
            legacyTopics.isNotEmpty() -> legacyTopics + RssFeedDefaults.coreTopicKeys
            else -> RssFeedDefaults.defaultEnabledTopicKeys()
        }
        val tickerSources = if (storedTickerSources.isNotEmpty()) {
            storedTickerSources
        } else if (!hasStoredTickerSources) {
            RssFeedDefaults.defaultTickerSourceIds
        } else {
            storedTickerSources
        }

        AppSettings(
            quoteRefreshIntervalMinutes = prefs.getInt(KEY_QUOTE_REFRESH, 5),
            alertCheckIntervalMinutes = prefs.getInt(KEY_ALERT_INTERVAL, 15),
            vwapDipPercent = prefs.getFloat(KEY_VWAP_DIP, 1.0f).toDouble(),
            rsiOversold = prefs.getFloat(KEY_RSI_OVERSOLD, 30.0f).toDouble(),
            rsiOverbought = prefs.getFloat(KEY_RSI_OVERBOUGHT, 70.0f).toDouble(),
            useMockDataWhenOffline = prefs.getBoolean(KEY_MOCK_DATA, true),
            cacheTtlQuotesMinutes = prefs.getInt(KEY_CACHE_QUOTES, 1),
            cacheTtlIntradayMinutes = prefs.getInt(KEY_CACHE_INTRADAY, 10),
            cacheTtlDailyMinutes = prefs.getInt(KEY_CACHE_DAILY, 1440),
            cacheTtlProfileMinutes = prefs.getInt(KEY_CACHE_PROFILE, 1440),
            cacheTtlMetricsMinutes = prefs.getInt(KEY_CACHE_METRICS, 1440),
            cacheTtlSentimentMinutes = prefs.getInt(KEY_CACHE_SENTIMENT, 30),
            aiSummaryPrefetchEnabled = prefs.getBoolean(KEY_AI_PREFETCH_ENABLED, true),
            aiSummaryPrefetchLimit = prefs.getInt(KEY_AI_PREFETCH_LIMIT, 3),
            verboseLogging = prefs.getBoolean(KEY_VERBOSE_LOGGING, true),
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
            discoveryMode = parseDiscoveryMode(prefs.getString(KEY_DISCOVERY_MODE, "STATIC")),
            isAnalysisPaused = prefs.getBoolean(KEY_ANALYSIS_PAUSED, false),
            useStagedPipeline = prefs.getBoolean(KEY_USE_STAGED, false),
            themeMode = com.polaralias.signalsynthesis.data.settings.ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"),
            deepDiveProvider = com.polaralias.signalsynthesis.domain.ai.LlmProvider.valueOf(prefs.getString(KEY_DEEP_DIVE_PROVIDER, "OPENAI") ?: "OPENAI"),
            modelRouting = com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig.fromJson(prefs.getString(KEY_MODEL_ROUTING, null)),
            rssEnabledTopics = enabledTopics,
            rssTickerSources = tickerSources,
            rssUseTickerFeedsForFinalStage = prefs.getBoolean(KEY_RSS_TICKER_FINAL_STAGE, true),
            rssApplyExpandedToAll = prefs.getBoolean(KEY_RSS_EXPANDED_ALL, false)
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
            putInt(KEY_CACHE_QUOTES, settings.cacheTtlQuotesMinutes)
            putInt(KEY_CACHE_INTRADAY, settings.cacheTtlIntradayMinutes)
            putInt(KEY_CACHE_DAILY, settings.cacheTtlDailyMinutes)
            putInt(KEY_CACHE_PROFILE, settings.cacheTtlProfileMinutes)
            putInt(KEY_CACHE_METRICS, settings.cacheTtlMetricsMinutes)
            putInt(KEY_CACHE_SENTIMENT, settings.cacheTtlSentimentMinutes)
            putBoolean(KEY_AI_PREFETCH_ENABLED, settings.aiSummaryPrefetchEnabled)
            putInt(KEY_AI_PREFETCH_LIMIT, settings.aiSummaryPrefetchLimit)
            putBoolean(KEY_VERBOSE_LOGGING, settings.verboseLogging)
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
            putString(KEY_DEEP_DIVE_PROVIDER, settings.deepDiveProvider.name)
            putString(KEY_MODEL_ROUTING, settings.modelRouting.toJson())
            putString(KEY_RSS_ENABLED_TOPICS, stringListAdapter.toJson(settings.rssEnabledTopics.toList()))
            putString(KEY_RSS_TICKER_SOURCES, stringListAdapter.toJson(settings.rssTickerSources.toList()))
            putBoolean(KEY_RSS_TICKER_FINAL_STAGE, settings.rssUseTickerFeedsForFinalStage)
            putBoolean(KEY_RSS_EXPANDED_ALL, settings.rssApplyExpandedToAll)
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
        private const val KEY_CACHE_QUOTES = "cache_ttl_quotes"
        private const val KEY_CACHE_INTRADAY = "cache_ttl_intraday"
        private const val KEY_CACHE_DAILY = "cache_ttl_daily"
        private const val KEY_CACHE_PROFILE = "cache_ttl_profile"
        private const val KEY_CACHE_METRICS = "cache_ttl_metrics"
        private const val KEY_CACHE_SENTIMENT = "cache_ttl_sentiment"
        private const val KEY_AI_PREFETCH_ENABLED = "ai_prefetch_enabled"
        private const val KEY_AI_PREFETCH_LIMIT = "ai_prefetch_limit"
        private const val KEY_VERBOSE_LOGGING = "verbose_logging"
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
        private const val KEY_DEEP_DIVE_PROVIDER = "deep_dive_provider"
        private const val KEY_MODEL_ROUTING = "model_routing_by_stage"
        private const val KEY_RSS_FEEDS = "user_rss_feeds"
        private const val KEY_RSS_ENABLED_TOPICS = "rss_enabled_topics"
        private const val KEY_RSS_TICKER_SOURCES = "rss_ticker_sources"
        private const val KEY_RSS_TICKER_FINAL_STAGE = "rss_ticker_final_stage"
        private const val KEY_RSS_EXPANDED_ALL = "rss_expanded_all"
    }

    private fun migrateLegacyTopics(): Set<String> {
        val legacy = parseStringList(prefs.getString(KEY_RSS_FEEDS, "[]"))
        if (legacy.isEmpty()) return emptySet()
        val urlToKey = rssCatalog.entries.associate { entry -> entry.url to entry.topicKey }
        return legacy.mapNotNull { urlToKey[it] }.toSet()
    }

    private fun parseStringList(raw: String?): List<String> {
        return try {
            stringListAdapter.fromJson(raw ?: "[]") ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

private fun parseDiscoveryMode(value: String?): com.polaralias.signalsynthesis.data.settings.DiscoveryMode {
    return when (value?.uppercase()) {
        "CURATED", "STATIC" -> com.polaralias.signalsynthesis.data.settings.DiscoveryMode.STATIC
        "LIVE_SCANNER", "SCREENER" -> com.polaralias.signalsynthesis.data.settings.DiscoveryMode.SCREENER
        "CUSTOM", "CUSTOM_ONLY" -> com.polaralias.signalsynthesis.data.settings.DiscoveryMode.CUSTOM
        else -> com.polaralias.signalsynthesis.data.settings.DiscoveryMode.STATIC
    }
}
