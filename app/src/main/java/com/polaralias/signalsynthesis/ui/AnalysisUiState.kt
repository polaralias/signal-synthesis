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
    val polygonKey: String = "",
    val finnhubKey: String = "",
    val fmpKey: String = "",
    val llmKey: String = ""
) {
    fun toApiKeys(): ApiKeys = ApiKeys(
        alpacaKey = alpacaKey.ifBlank { null },
        alpacaSecret = alpacaSecret.ifBlank { null },
        polygon = polygonKey.ifBlank { null },
        finnhub = finnhubKey.ifBlank { null },
        financialModelingPrep = fmpKey.ifBlank { null }
    )

    companion object {
        fun from(apiKeys: ApiKeys, llmKey: String?): ApiKeyUiState = ApiKeyUiState(
            alpacaKey = apiKeys.alpacaKey.orEmpty(),
            alpacaSecret = apiKeys.alpacaSecret.orEmpty(),
            polygonKey = apiKeys.polygon.orEmpty(),
            finnhubKey = apiKeys.finnhub.orEmpty(),
            fmpKey = apiKeys.financialModelingPrep.orEmpty(),
            llmKey = llmKey.orEmpty()
        )
    }
}

data class AnalysisUiState(
    val intent: TradingIntent = TradingIntent.DAY_TRADE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val result: AnalysisResult? = null,
    val lastRunAt: Instant? = null,
    val keys: ApiKeyUiState = ApiKeyUiState(),
    val hasAnyApiKeys: Boolean = false,
    val hasLlmKey: Boolean = false,
    val alertsEnabled: Boolean = false,
    val alertSymbolCount: Int = 0,
    val watchlist: List<String> = emptyList(),
    val history: List<AnalysisResult> = emptyList(),
    val appSettings: AppSettings = AppSettings(),
    val aiThresholdSuggestion: AiThresholdSuggestion? = null,
    val isSuggestingThresholds: Boolean = false,
    val isPrefetching: Boolean = false,
    val prefetchCount: Int = 0,
    val aiSummaries: Map<String, AiSummaryState> = emptyMap(),
    val marketOverview: MarketOverview? = null,
    val isLoadingMarket: Boolean = false
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
