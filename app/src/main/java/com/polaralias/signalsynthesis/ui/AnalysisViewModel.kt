package com.polaralias.signalsynthesis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.signalsynthesis.data.provider.MarketDataProviderFactory
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStorage
import com.polaralias.signalsynthesis.data.repository.AiSummaryRepository
import com.polaralias.signalsynthesis.data.repository.DatabaseRepository
import com.polaralias.signalsynthesis.data.settings.AppSettings
import com.polaralias.signalsynthesis.data.storage.ApiKeyStorage
import com.polaralias.signalsynthesis.data.storage.AppSettingsStorage
import com.polaralias.signalsynthesis.data.worker.WorkScheduler
import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.usecase.PrefetchAiSummariesUseCase
import com.polaralias.signalsynthesis.domain.usecase.RunAnalysisUseCase
import com.polaralias.signalsynthesis.domain.usecase.SynthesizeSetupUseCase
import com.polaralias.signalsynthesis.util.Logger
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.IndexQuote
import com.polaralias.signalsynthesis.domain.model.MarketOverview
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

class AnalysisViewModel(
    private val providerFactory: MarketDataProviderFactory,
    private val keyStore: ApiKeyStorage,
    private val alertStore: AlertSettingsStorage,
    private val workScheduler: WorkScheduler,
    private val llmClient: LlmClient,
    private val dbRepository: DatabaseRepository,
    private val appSettingsStore: AppSettingsStorage,
    private val aiSummaryRepository: AiSummaryRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        refreshKeys()
        refreshAlerts()
        observeWatchlist()
        observeHistory()
        observeAppSettings()
        refreshMarketOverview()
    }

    fun updateIntent(intent: TradingIntent) {
        _uiState.update { it.copy(intent = intent) }
    }

    fun updateKey(field: KeyField, value: String) {
        _uiState.update { state ->
            val keys = when (field) {
                KeyField.ALPACA_KEY -> state.keys.copy(alpacaKey = value)
                KeyField.ALPACA_SECRET -> state.keys.copy(alpacaSecret = value)
                KeyField.POLYGON -> state.keys.copy(polygonKey = value)
                KeyField.FINNHUB -> state.keys.copy(finnhubKey = value)
                KeyField.FMP -> state.keys.copy(fmpKey = value)
                KeyField.LLM -> state.keys.copy(llmKey = value)
            }
            state.copy(keys = keys)
        }
    }

    fun saveKeys() {
        viewModelScope.launch(ioDispatcher) {
            val keys = _uiState.value.keys
            keyStore.saveKeys(keys.toApiKeys(), keys.llmKey)
            Logger.event("keys_saved", mapOf("has_llm_key" to (keys.llmKey != null)))
            refreshKeys()
        }
    }

    fun clearKeys() {
        viewModelScope.launch(ioDispatcher) {
            keyStore.clear()
            refreshKeys()
        }
    }

    fun runAnalysis() {
        val apiKeys = _uiState.value.keys.toApiKeys()
        if (!apiKeys.hasAny()) {
            _uiState.update { it.copy(errorMessage = "Add at least one provider key before running analysis.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, aiSummaries = emptyMap()) }
        Logger.event("analysis_started", mapOf("intent" to _uiState.value.intent.name))
        viewModelScope.launch(ioDispatcher) {
            try {
                val useCase = buildUseCase(apiKeys)
                val result = useCase.execute(_uiState.value.intent)
                Logger.event("analysis_completed", mapOf("setups" to result.setups.size))
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = result,
                        lastRunAt = result.generatedAt
                    )
                }
                dbRepository.saveHistory(result)
                val symbols = result.setups.map { it.symbol }.distinct()
                alertStore.saveSymbols(symbols)
                _uiState.update { it.copy(alertSymbolCount = symbols.size) }

                // Trigger Prefetching
                val llmKey = _uiState.value.keys.llmKey
                if (llmKey.isNotBlank() && result.setups.isNotEmpty()) {
                    _uiState.update { it.copy(isPrefetching = true, prefetchCount = 0) }
                    
                    val prefetchFlow = PrefetchAiSummariesUseCase(
                        buildSynthesisUseCase(apiKeys),
                        aiSummaryRepository
                    ).execute(result.setups, llmKey)

                    prefetchFlow.collect { (symbol, synthesis) ->
                        updateAiSummary(
                            symbol,
                            AiSummaryState(
                                status = AiSummaryStatus.READY,
                                summary = synthesis.summary,
                                risks = synthesis.risks,
                                verdict = synthesis.verdict
                            )
                        )
                        _uiState.update { it.copy(prefetchCount = it.prefetchCount + 1) }
                    }
                    _uiState.update { it.copy(isPrefetching = false) }
                }
            } catch (ex: Exception) {
                Logger.e("ViewModel", "Analysis failed", ex)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ex.message ?: "Analysis failed. Please try again."
                    )
                }
            }
        }
    }

    fun showHistoricalResult(result: AnalysisResult) {
        _uiState.update {
            it.copy(
                result = result,
                lastRunAt = result.generatedAt,
                aiSummaries = emptyMap()
            )
        }
    }

    fun requestAiSummary(symbol: String) {
        val state = _uiState.value
        val llmKey = state.keys.llmKey
        if (llmKey.isBlank() || !state.hasLlmKey) return
        val setup = state.result?.setups?.firstOrNull { it.symbol == symbol } ?: return
        val existing = state.aiSummaries[symbol]
        if (existing?.status == AiSummaryStatus.LOADING || existing?.status == AiSummaryStatus.READY) return

        updateAiSummary(symbol, AiSummaryState(status = AiSummaryStatus.LOADING))
        viewModelScope.launch(ioDispatcher) {
            try {
                // Check cache first
                val cached = aiSummaryRepository.getSummary(symbol)
                if (cached != null) {
                    updateAiSummary(
                        symbol,
                        AiSummaryState(
                            status = AiSummaryStatus.READY,
                            summary = cached.summary,
                            risks = cached.risks,
                            verdict = cached.verdict
                        )
                    )
                    return@launch
                }

                val useCase = buildSynthesisUseCase(state.keys.toApiKeys())
                val synthesis = useCase.execute(setup, llmKey)
                
                // Save to cache
                aiSummaryRepository.saveSummary(symbol, synthesis)

                updateAiSummary(
                    symbol,
                    AiSummaryState(
                        status = AiSummaryStatus.READY,
                        summary = synthesis.summary,
                        risks = synthesis.risks,
                        verdict = synthesis.verdict
                    )
                )
            } catch (ex: Exception) {
                updateAiSummary(
                    symbol,
                    AiSummaryState(
                        status = AiSummaryStatus.ERROR,
                        errorMessage = ex.message ?: "AI synthesis failed."
                    )
                )
            }
        }
    }

    fun suggestThresholdsWithAi(prompt: String) {
        val llmKey = _uiState.value.keys.llmKey
        if (llmKey.isBlank()) return

        _uiState.update { it.copy(isSuggestingThresholds = true) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val systemPrompt = """
                    You are a financial alert threshold optimizer. Based on the user's input, suggest optimal values for:
                    1. VWAP Dip %: How far below VWAP price should be to trigger an alert (typical 0.5 to 5.0).
                    2. RSI Oversold: Threshold for oversold (typical 20 to 40).
                    3. RSI Overbought: Threshold for overbought (typical 60 to 80).
                    
                    Return ONLY a JSON object with fields: vwapDipPercent, rsiOversold, rsiOverbought, rationale.
                    Rationale should be a concise sentence explaining the choice based on their context.
                """.trimIndent()

                val response = llmClient.generate(
                    prompt = "User context: $prompt",
                    systemPrompt = systemPrompt,
                    apiKey = llmKey
                )

                val suggestion = parseAiThresholdSuggestion(response)
                _uiState.update { it.copy(aiThresholdSuggestion = suggestion, isSuggestingThresholds = false) }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isSuggestingThresholds = false, errorMessage = "AI suggestion failed: ${ex.message}") }
            }
        }
    }

    fun applyAiThresholdSuggestion() {
        _uiState.value.aiThresholdSuggestion?.let { suggestion ->
            val updated = _uiState.value.appSettings.copy(
                vwapDipPercent = suggestion.vwapDipPercent,
                rsiOversold = suggestion.rsiOversold,
                rsiOverbought = suggestion.rsiOverbought
            )
            updateAppSettings(updated)
            _uiState.update { it.copy(aiThresholdSuggestion = null) }
        }
    }

    fun updateAppSettings(settings: AppSettings) {
        viewModelScope.launch(ioDispatcher) {
            appSettingsStore.saveSettings(settings)
            
            val currentAlerts = alertStore.loadSettings()
            alertStore.saveSettings(currentAlerts.copy(
                vwapDipPercent = settings.vwapDipPercent,
                rsiOversold = settings.rsiOversold,
                rsiOverbought = settings.rsiOverbought
            ))
            
            _uiState.update { it.copy(appSettings = settings) }
        }
    }

    fun dismissAiSuggestion() {
        _uiState.update { it.copy(aiThresholdSuggestion = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun updateAlertsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(alertsEnabled = enabled) }
        viewModelScope.launch(ioDispatcher) {
            val current = alertStore.loadSettings()
            alertStore.saveSettings(current.copy(enabled = enabled))
            workScheduler.scheduleAlerts(enabled)
        }
    }

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch(ioDispatcher) {
            val current = _uiState.value.watchlist
            if (current.contains(symbol)) {
                dbRepository.removeFromWatchlist(symbol)
            } else {
                dbRepository.addToWatchlist(symbol)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(ioDispatcher) {
            dbRepository.clearHistory()
        }
    }

    private fun observeWatchlist() {
        viewModelScope.launch(ioDispatcher) {
            dbRepository.getWatchlist().collect { list ->
                _uiState.update { it.copy(watchlist = list) }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch(ioDispatcher) {
            dbRepository.getHistory().collect { list ->
                _uiState.update { it.copy(history = list) }
            }
        }
    }

    private fun refreshKeys() {
        viewModelScope.launch(ioDispatcher) {
            val apiKeys = keyStore.loadApiKeys()
            val llmKey = keyStore.loadLlmKey()
            _uiState.update {
                it.copy(
                    keys = ApiKeyUiState.from(apiKeys, llmKey),
                    hasAnyApiKeys = apiKeys.hasAny(),
                    hasLlmKey = !llmKey.isNullOrBlank()
                )
            }
        }
    }

    private fun refreshAlerts() {
        viewModelScope.launch(ioDispatcher) {
            val settings = alertStore.loadSettings()
            val symbols = alertStore.loadSymbols()
            _uiState.update {
                it.copy(
                    alertsEnabled = settings.enabled,
                    alertSymbolCount = symbols.size
                )
            }
            workScheduler.scheduleAlerts(settings.enabled)
        }
    }

    private fun buildUseCase(apiKeys: com.polaralias.signalsynthesis.data.provider.ApiKeys): RunAnalysisUseCase {
        val bundle = providerFactory.build(apiKeys)
        val repository = MarketDataRepository(bundle)
        return RunAnalysisUseCase(repository, clock)
    }

    fun refreshMarketOverview() {
        val state = _uiState.value
        if (state.isLoadingMarket) return

        _uiState.update { it.copy(isLoadingMarket = true) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val apiKeys = _uiState.value.keys.toApiKeys()
                val bundle = providerFactory.build(apiKeys)
                val repository = MarketDataRepository(bundle)
                
                val symbols = listOf("SPY", "QQQ", "DIA")
                val quotes = repository.getQuotes(symbols)
                
                val indices = symbols.mapNotNull { symbol ->
                    val quote = quotes[symbol] ?: return@mapNotNull null
                    val names = mapOf("SPY" to "S&P 500", "QQQ" to "Nasdaq 100", "DIA" to "Dow 30")
                    IndexQuote(
                        symbol = symbol,
                        name = names[symbol] ?: symbol,
                        price = quote.price,
                        changePercent = quote.changePercent ?: 0.0
                    )
                }

                if (indices.isNotEmpty()) {
                    _uiState.update { 
                        it.copy(
                            marketOverview = MarketOverview(indices, clock.instant()),
                            isLoadingMarket = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingMarket = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMarket = false) }
            }
        }
    }

    private fun buildSynthesisUseCase(
        apiKeys: com.polaralias.signalsynthesis.data.provider.ApiKeys
    ): SynthesizeSetupUseCase {
        val bundle = providerFactory.build(apiKeys)
        val repository = MarketDataRepository(bundle)
        return SynthesizeSetupUseCase(repository, llmClient)
    }

    private fun updateAiSummary(symbol: String, summary: AiSummaryState) {
        _uiState.update { state ->
            state.copy(aiSummaries = state.aiSummaries + (symbol to summary))
        }
    }

    private fun observeAppSettings() {
        viewModelScope.launch(ioDispatcher) {
            val settings = appSettingsStore.loadSettings()
            _uiState.update { it.copy(appSettings = settings) }
        }
    }

    private fun parseAiThresholdSuggestion(json: String): AiThresholdSuggestion {
        // Very basic manual parsing for demonstration. In a real app, use Moshi/Gson.
        val vwap = Regex("\"vwapDipPercent\":\\s*([0-9.]+)").find(json)?.groupValues?.get(1)?.toDouble() ?: 1.0
        val oversold = Regex("\"rsiOversold\":\\s*([0-9.]+)").find(json)?.groupValues?.get(1)?.toDouble() ?: 30.0
        val overbought = Regex("\"rsiOverbought\":\\s*([0-9.]+)").find(json)?.groupValues?.get(1)?.toDouble() ?: 70.0
        val rationale = Regex("\"rationale\":\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: "Suggested based on your input."
        
        return AiThresholdSuggestion(vwap, oversold, overbought, rationale)
    }
}

enum class KeyField {
    ALPACA_KEY,
    ALPACA_SECRET,
    POLYGON,
    FINNHUB,
    FMP,
    LLM
}
