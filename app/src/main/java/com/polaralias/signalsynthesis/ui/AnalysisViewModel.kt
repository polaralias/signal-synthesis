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
import com.polaralias.signalsynthesis.data.ai.LlmClientFactory
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
import java.time.ZoneId

class AnalysisViewModel(
    private val providerFactory: MarketDataProviderFactory,
    private val keyStore: ApiKeyStorage,
    private val alertStore: AlertSettingsStorage,
    private val workScheduler: WorkScheduler,
    private val llmClientFactory: LlmClientFactory,
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
        observeMonthlyUsage()
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
                KeyField.TWELVE_DATA -> state.keys.copy(twelveDataKey = value)
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

    fun addCustomTicker(symbol: String) {
        val upper = symbol.trim().uppercase()
        if (upper.isBlank()) return
        val current = _uiState.value.customTickers
        if (current.any { it.symbol == upper }) return
        
        val newList = current + TickerEntry(upper, true)
        _uiState.update { it.copy(customTickers = newList) }
        saveCustomTickers(newList.map { it.symbol })
    }

    fun removeCustomTicker(symbol: String) {
        val newList = _uiState.value.customTickers.filter { it.symbol != symbol }
        _uiState.update { it.copy(customTickers = newList) }
        saveCustomTickers(newList.map { it.symbol })
    }

    private fun saveCustomTickers(tickers: List<String>) {
        viewModelScope.launch(ioDispatcher) {
            (appSettingsStore as? AppSettingsStore)?.saveCustomTickers(tickers)
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
                val state = _uiState.value
                val customTickerList = state.customTickers.map { it.symbol }
                
                Logger.event("analysis_input", mapOf(
                    "custom_tickers_count" to customTickerList.size,
                    "provider_screener_enabled" to (state.hasAnyApiKeys)
                ))

                val result = useCase.execute(
                    intent = state.intent,
                    risk = state.appSettings.riskTolerance,
                    customTickers = customTickerList,
                    screenerThresholds = mapOf(
                        "conservative" to state.appSettings.screenerConservativeThreshold,
                        "moderate" to state.appSettings.screenerModerateThreshold,
                        "aggressive" to state.appSettings.screenerAggressiveThreshold
                    )
                )
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
                val synthesis = useCase.execute(
                    setup = setup,
                    llmKey = llmKey,
                    reasoningDepth = state.appSettings.reasoningDepth,
                    outputLength = state.appSettings.outputLength,
                    verbosity = state.appSettings.verbosity
                )
                
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

    fun requestChartData(symbol: String) {
        val state = _uiState.value
        val setup = state.result?.setups?.firstOrNull { it.symbol == symbol } ?: return
        val existing = state.chartData[symbol]
        if (existing?.status == ChartStatus.LOADING || existing?.status == ChartStatus.READY) return

        updateChartData(symbol, ChartState(status = ChartStatus.LOADING))
        viewModelScope.launch(ioDispatcher) {
            try {
                val apiKeys = _uiState.value.keys.toApiKeys()
                val bundle = providerFactory.build(apiKeys)
                val repository = MarketDataRepository(bundle)

                val points = when (setup.intent) {
                    TradingIntent.DAY_TRADE -> {
                        repository.getIntraday(symbol, days = 2).map { 
                            PricePoint(it.time, it.close) 
                        }
                    }
                    TradingIntent.SWING, TradingIntent.LONG_TERM -> {
                        repository.getDaily(symbol, days = 200).map { 
                            PricePoint(it.date.atStartOfDay(ZoneId.systemDefault()).toInstant(), it.close) 
                        }
                    }
                }

                if (points.isNotEmpty()) {
                    updateChartData(
                        symbol,
                        ChartState(
                            status = ChartStatus.READY,
                            points = points
                        )
                    )
                } else {
                    updateChartData(
                        symbol,
                        ChartState(
                            status = ChartStatus.ERROR,
                            errorMessage = "No price data returned from providers."
                        )
                    )
                }
            } catch (ex: Exception) {
                Logger.e("ViewModel", "Failed to fetch chart data for $symbol", ex)
                updateChartData(
                    symbol,
                    ChartState(
                        status = ChartStatus.ERROR,
                        errorMessage = ex.message ?: "Failed to load chart data."
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
                val systemPrompt = com.polaralias.signalsynthesis.domain.ai.AiPrompts.THRESHOLD_SUGGESTION_SYSTEM.trimIndent()

                val currentModel = _uiState.value.appSettings.llmModel
                val client = llmClientFactory.create(currentModel)
                
                val settings = _uiState.value.appSettings
                val startTime = System.currentTimeMillis()
                val response = client.generate(
                    prompt = "User context: $prompt",
                    systemPrompt = systemPrompt,
                    apiKey = llmKey,
                    reasoningDepth = settings.reasoningDepth,
                    outputLength = settings.outputLength,
                    verbosity = settings.verbosity
                )
                val duration = System.currentTimeMillis() - startTime
                com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("ThresholdSuggestion", prompt, response, true, duration)

                val suggestion = parseAiThresholdSuggestion(response)
                _uiState.update { it.copy(aiThresholdSuggestion = suggestion, isSuggestingThresholds = false) }
            } catch (ex: Exception) {
                com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("ThresholdSuggestion", prompt, ex.message ?: "Failed", false, 0)
                _uiState.update { it.copy(isSuggestingThresholds = false, errorMessage = "AI suggestion failed: ${ex.message}") }
            }
        }
    }

    fun suggestScreenerWithAi(prompt: String) {
        val llmKey = _uiState.value.keys.llmKey
        if (llmKey.isBlank()) return

        _uiState.update { it.copy(isSuggestingScreener = true) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val systemPrompt = """
                    You are a stock market expert. Based on the user's trading style, suggest price thresholds for Conservative, Moderate, and Aggressive stock screening.
                    Conservative: Low volatility, steady stocks (usually lower price/market cap limits or higher quality).
                    Moderate: Balanced risk.
                    Aggressive: High risk/reward, potentially low-priced or high-volatility stocks.
                    
                    Return a JSON object:
                    {
                      "conservativeLimit": double,
                      "moderateLimit": double,
                      "aggressiveLimit": double,
                      "rationale": "short explanation"
                    }
                """.trimIndent()

                val settings = _uiState.value.appSettings
                val client = llmClientFactory.create(settings.llmModel)
                val startTime = System.currentTimeMillis()
                val response = client.generate(
                    prompt = "User context: $prompt",
                    systemPrompt = systemPrompt,
                    apiKey = llmKey,
                    reasoningDepth = settings.reasoningDepth,
                    outputLength = settings.outputLength,
                    verbosity = settings.verbosity
                )
                val duration = System.currentTimeMillis() - startTime
                com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("ScreenerSuggestion", prompt, response, true, duration)

                val suggestion = parseAiScreenerSuggestion(response)
                _uiState.update { it.copy(aiScreenerSuggestion = suggestion, isSuggestingScreener = false) }
            } catch (ex: Exception) {
                com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("ScreenerSuggestion", prompt, ex.message ?: "Failed", false, 0)
                _uiState.update { it.copy(isSuggestingScreener = false, errorMessage = "AI suggestion failed: ${ex.message}") }
            }
        }
    }

    fun applyAiScreenerSuggestion() {
        _uiState.value.aiScreenerSuggestion?.let { suggestion ->
            val updated = _uiState.value.appSettings.copy(
                screenerConservativeThreshold = suggestion.conservativeLimit,
                screenerModerateThreshold = suggestion.moderateLimit,
                screenerAggressiveThreshold = suggestion.aggressiveLimit
            )
            updateAppSettings(updated)
            _uiState.update { it.copy(aiScreenerSuggestion = null) }
        }
    }

    fun dismissAiScreenerSuggestion() {
        _uiState.update { it.copy(aiScreenerSuggestion = null) }
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
        val oldInterval = _uiState.value.appSettings.alertCheckIntervalMinutes
        viewModelScope.launch(ioDispatcher) {
            appSettingsStore.saveSettings(settings)
            
            val currentAlerts = alertStore.loadSettings()
            alertStore.saveSettings(currentAlerts.copy(
                vwapDipPercent = settings.vwapDipPercent,
                rsiOversold = settings.rsiOversold,
                rsiOverbought = settings.rsiOverbought
            ))
            
            _uiState.update { it.copy(appSettings = settings) }
            
            if (_uiState.value.alertsEnabled && oldInterval != settings.alertCheckIntervalMinutes) {
                workScheduler.scheduleAlerts(true, settings.alertCheckIntervalMinutes)
            }
        }
    }

    fun searchTickers(query: String) {
        if (query.length < 2) {
            _uiState.update { it.copy(tickerSearchResults = emptyList()) }
            return
        }
        
        _uiState.update { it.copy(isSearchingTickers = true) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val apiKeys = _uiState.value.keys.toApiKeys()
                val bundle = providerFactory.build(apiKeys)
                val repository = MarketDataRepository(bundle)
                val results = repository.searchSymbols(query)
                _uiState.update { it.copy(tickerSearchResults = results, isSearchingTickers = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingTickers = false) }
            }
        }
    }

    fun clearTickerSearch() {
        _uiState.update { it.copy(tickerSearchResults = emptyList()) }
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
            workScheduler.scheduleAlerts(enabled, _uiState.value.appSettings.alertCheckIntervalMinutes)
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
        val client = llmClientFactory.create(_uiState.value.appSettings.llmModel)
        return SynthesizeSetupUseCase(repository, client)
    }

    private fun updateAiSummary(symbol: String, summary: AiSummaryState) {
        _uiState.update { state ->
            state.copy(aiSummaries = state.aiSummaries + (symbol to summary))
        }
    }

    private fun updateChartData(symbol: String, chartState: ChartState) {
        _uiState.update { state ->
            state.copy(chartData = state.chartData + (symbol to chartState))
        }
    }

    private fun observeAppSettings() {
        viewModelScope.launch(ioDispatcher) {
            val settings = appSettingsStore.loadSettings()
            val custom = (appSettingsStore as? AppSettingsStore)?.loadCustomTickers() ?: emptyList()
            _uiState.update { 
                it.copy(
                    appSettings = settings,
                    customTickers = custom.map { TickerEntry(it, true) }
                ) 
            }
        }
    }

    private fun observeMonthlyUsage() {
        viewModelScope.launch {
            com.polaralias.signalsynthesis.util.UsageTracker.monthlyApiCount.collect { count ->
                _uiState.update { it.copy(monthlyApiUsage = count) }
            }
        }
        viewModelScope.launch {
            com.polaralias.signalsynthesis.util.UsageTracker.monthlyProviderUsage.collect { usage ->
                _uiState.update { it.copy(monthlyProviderUsage = usage) }
            }
        }
    }

    private fun parseAiThresholdSuggestion(json: String): AiThresholdSuggestion {
        try {
            val moshi = com.squareup.moshi.Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(AiThresholdSuggestion::class.java)
            
            val validJson = json.replace(Regex("```json|```"), "").trim()
            return adapter.fromJson(validJson) ?: AiThresholdSuggestion(1.0, 30.0, 70.0, "Could not parse suggestion.")
        } catch (e: Exception) {
            Logger.e("AnalysisViewModel", "Failed to parse AI suggestion", e)
            return AiThresholdSuggestion(1.0, 30.0, 70.0, "Failed to parse AI suggestion.")
        }
    }

    private fun parseAiScreenerSuggestion(json: String): AiScreenerSuggestion {
        try {
            val moshi = com.squareup.moshi.Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(AiScreenerSuggestion::class.java)
            
            val validJson = json.replace(Regex("```json|```"), "").trim()
            return adapter.fromJson(validJson) ?: AiScreenerSuggestion(5.0, 20.0, 100.0, "Could not parse suggestion.")
        } catch (e: Exception) {
            Logger.e("AnalysisViewModel", "Failed to parse AI screener suggestion", e)
            return AiScreenerSuggestion(5.0, 20.0, 100.0, "Failed to parse AI suggestion.")
        }
    }
}

enum class KeyField {
    ALPACA_KEY,
    ALPACA_SECRET,
    POLYGON,
    FINNHUB,
    FMP,
    TWELVE_DATA,
    LLM
}
