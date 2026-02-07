package com.polaralias.signalsynthesis.data.settings

import com.polaralias.signalsynthesis.domain.ai.LlmModel
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.ReasoningDepth
import com.polaralias.signalsynthesis.domain.ai.OutputLength
import com.polaralias.signalsynthesis.domain.ai.Verbosity
import com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig
import com.polaralias.signalsynthesis.data.rss.RssFeedDefaults

data class AppSettings(
    val quoteRefreshIntervalMinutes: Int = 5,
    val alertCheckIntervalMinutes: Int = 15,
    val vwapDipPercent: Double = 1.0,
    val rsiOversold: Double = 30.0,
    val rsiOverbought: Double = 70.0,
    val useMockDataWhenOffline: Boolean = true,
    val cacheTtlQuotesMinutes: Int = 1,
    val cacheTtlIntradayMinutes: Int = 10,
    val cacheTtlDailyMinutes: Int = 1440,
    val cacheTtlProfileMinutes: Int = 1440,
    val cacheTtlMetricsMinutes: Int = 1440,
    val cacheTtlSentimentMinutes: Int = 30,
    val aiSummaryPrefetchEnabled: Boolean = true,
    val aiSummaryPrefetchLimit: Int = 3,
    val verboseLogging: Boolean = true,
    val llmProvider: LlmProvider = LlmProvider.OPENAI,
    val analysisModel: LlmModel = LlmModel.GPT_5_1,
    val verdictModel: LlmModel = LlmModel.GPT_5_1,
    val reasoningModel: LlmModel = LlmModel.GPT_5_2,
    val reasoningDepth: ReasoningDepth = ReasoningDepth.MEDIUM,
    val outputLength: OutputLength = OutputLength.STANDARD,
    val verbosity: Verbosity = Verbosity.MEDIUM,
    val riskTolerance: RiskTolerance = RiskTolerance.MODERATE,
    val screenerConservativeThreshold: Double = 5.0, // e.g. Max Price for conservative
    val screenerModerateThreshold: Double = 20.0,
    val screenerAggressiveThreshold: Double = 100.0,
    val screenerMinVolume: Long = 1_000_000L,
    val preferredAssetClass: AssetClass = AssetClass.STOCKS,
    val discoveryMode: DiscoveryMode = DiscoveryMode.STATIC,
    val isAnalysisPaused: Boolean = false,
    val useStagedPipeline: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val deepDiveProvider: LlmProvider = LlmProvider.OPENAI,
    val modelRouting: UserModelRoutingConfig = UserModelRoutingConfig(),
    val rssEnabledTopics: Set<String> = RssFeedDefaults.defaultEnabledTopicKeys(),
    val rssTickerSources: Set<String> = RssFeedDefaults.defaultTickerSourceIds,
    val rssUseTickerFeedsForFinalStage: Boolean = true,
    val rssApplyExpandedToAll: Boolean = false,
    val aiSuggestedSettingsLocked: Boolean = false
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class RiskTolerance { CONSERVATIVE, MODERATE, AGGRESSIVE }

enum class AssetClass { STOCKS, FOREX, METALS, ALL }

enum class DiscoveryMode { STATIC, SCREENER, CUSTOM }
