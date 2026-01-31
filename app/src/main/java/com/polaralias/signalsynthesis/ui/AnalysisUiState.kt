package com.polaralias.signalsynthesis.ui

import com.polaralias.signalsynthesis.data.provider.ApiKeys
import com.polaralias.signalsynthesis.data.settings.AppSettings
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.MarketOverview
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import java.time.Instant

data class ApiKeyUiState(
    val alpacaKey: String = "",
    val alpacaSecret: String = "",
    val massiveKey: String = "",
    val finnhubKey: String = "",
    val fmpKey: String = "",
    val twelveDataKey: String = "",
    val llmKey: String = ""
) {
    fun toApiKeys(): ApiKeys = ApiKeys(
        alpacaKey = alpacaKey.ifBlank { null },
        alpacaSecret = alpacaSecret.ifBlank { null },
        massive = massiveKey.ifBlank { null },
        finnhub = finnhubKey.ifBlank { null },
        financialModelingPrep = fmpKey.ifBlank { null },
        twelveData = twelveDataKey.ifBlank { null }
    )

    companion object {
        fun from(apiKeys: ApiKeys, llmKey: String?): ApiKeyUiState = ApiKeyUiState(
            alpacaKey = apiKeys.alpacaKey.orEmpty(),
            alpacaSecret = apiKeys.alpacaSecret.orEmpty(),
            massiveKey = apiKeys.massive.orEmpty(),
            finnhubKey = apiKeys.finnhub.orEmpty(),
            fmpKey = apiKeys.financialModelingPrep.orEmpty(),
            twelveDataKey = apiKeys.twelveData.orEmpty(),
            llmKey = llmKey.orEmpty()
        )
    }
}

data class TickerEntry(
    val symbol: String,
    val source: com.polaralias.signalsynthesis.domain.model.TickerSource = com.polaralias.signalsynthesis.domain.model.TickerSource.CUSTOM
)

data class AnalysisUiState(
    val intent: TradingIntent = TradingIntent.DAY_TRADE,
    val isLoading: Boolean = false,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val result: AnalysisResult? = null,
    val lastRunAt: Instant? = null,
    val keys: ApiKeyUiState = ApiKeyUiState(),
    val hasAnyApiKeys: Boolean = false,
    val hasLlmKey: Boolean = false,
    val alertsEnabled: Boolean = false,
    val alertSymbolCount: Int = 0,
    val alertSymbols: List<String> = emptyList(),
    val blocklist: List<String> = emptyList(),
    val removedAlerts: Set<String> = emptySet(),
    val watchlist: List<String> = emptyList(),
    val history: List<AnalysisResult> = emptyList(),
    val appSettings: AppSettings = AppSettings(),
    val aiThresholdSuggestion: AiThresholdSuggestion? = null,
    val isSuggestingThresholds: Boolean = false,
    val aiScreenerSuggestion: AiScreenerSuggestion? = null,
    val isSuggestingScreener: Boolean = false,
    val isPrefetching: Boolean = false,
    val prefetchCount: Int = 0,
    val aiSummaries: Map<String, AiSummaryState> = emptyMap(),
    val marketOverview: MarketOverview? = null,
    val isLoadingMarket: Boolean = false,
    val chartData: Map<String, ChartState> = emptyMap(),
    val customTickers: List<TickerEntry> = emptyList(),
    val tickerSearchResults: List<com.polaralias.signalsynthesis.domain.provider.SearchResult> = emptyList(),
    val isSearchingTickers: Boolean = false,
    val dailyApiUsage: Int = 0,
    val dailyProviderUsage: Map<String, Map<com.polaralias.signalsynthesis.util.ApiUsageCategory, Int>> = emptyMap(),
    val archivedUsage: List<com.polaralias.signalsynthesis.util.DailyUsageArchive> = emptyList(),
    val blacklistedProviders: List<String> = emptyList(),
    val assetClass: com.polaralias.signalsynthesis.data.settings.AssetClass = com.polaralias.signalsynthesis.data.settings.AssetClass.STOCKS,
    val deepDives: Map<String, DeepDiveState> = emptyMap(),
    val navigationEvent: NavigationEvent? = null,
    val isPaused: Boolean = false
)

sealed class NavigationEvent {
    object Results : NavigationEvent()
    object Alerts : NavigationEvent()
}

data class AiScreenerSuggestion(
    val conservativeLimit: Double,
    val moderateLimit: Double,
    val aggressiveLimit: Double,
    val minVolume: Long,
    val rationale: String
)

data class PricePoint(
    val timestamp: Instant,
    val price: Double
)

enum class ChartStatus {
    IDLE,
    LOADING,
    READY,
    ERROR
}

data class ChartState(
    val status: ChartStatus = ChartStatus.IDLE,
    val points: List<PricePoint> = emptyList(),
    val errorMessage: String? = null
)

data class AiThresholdSuggestion(
    val vwapDipPercent: Double,
    val rsiOversold: Double,
    val rsiOverbought: Double,
    val rationale: String
)

enum class AiSummaryStatus {
    IDLE,
    LOADING,
    READY,
    ERROR
}

data class AiSummaryState(
    val status: AiSummaryStatus = AiSummaryStatus.IDLE,
    val summary: String? = null,
    val risks: List<String> = emptyList(),
    val verdict: String? = null,
    val errorMessage: String? = null
)

enum class DeepDiveStatus {
    IDLE,
    LOADING,
    READY,
    ERROR
}

data class DeepDiveState(
    val status: DeepDiveStatus = DeepDiveStatus.IDLE,
    val data: com.polaralias.signalsynthesis.domain.model.DeepDive? = null,
    val errorMessage: String? = null
)
