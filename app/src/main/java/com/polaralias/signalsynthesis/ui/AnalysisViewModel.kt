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
import com.polaralias.signalsynthesis.data.storage.AppSettingsStore
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
import com.polaralias.signalsynthesis.domain.model.MarketSection
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
    private val application: android.app.Application,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var analysisJob: kotlinx.coroutines.Job? = null

    init {
        refreshKeys()
        refreshAlerts()
        observeWatchlist()
        observeHistory()
        observeAppSettings()
        refreshMarketOverview()
        observeDailyUsage()
        observeProviderBlacklist()
    }

    fun updateIntent(intent: TradingIntent) {
        _uiState.update { it.copy(intent = intent) }
    }

    fun updateAssetClass(assetClass: com.polaralias.signalsynthesis.data.settings.AssetClass) {
        _uiState.update { it.copy(assetClass = assetClass) }
    }

    fun updateDiscoveryMode(mode: com.polaralias.signalsynthesis.data.settings.DiscoveryMode) {
        val updated = _uiState.value.appSettings.copy(discoveryMode = mode)
        updateAppSettings(updated)
    }

    fun updateKey(field: KeyField, value: String) {
        _uiState.update { state ->
            val keys = when (field) {
                KeyField.ALPACA_KEY -> state.keys.copy(alpacaKey = value)
                KeyField.ALPACA_SECRET -> state.keys.copy(alpacaSecret = value)
                KeyField.MASSIVE -> state.keys.copy(massiveKey = value)
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
            Logger.event("keys_saved", mapOf("has_llm_key" to keys.llmKey.isNotBlank()))
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
        
        val newList = current + TickerEntry(upper)
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

        _uiState.update { it.copy(isLoading = true, errorMessage = null, aiSummaries = emptyMap(), isPaused = false, removedAlerts = emptySet()) }
        Logger.event("analysis_started", mapOf("intent" to _uiState.value.intent.name))
        analysisJob = viewModelScope.launch(ioDispatcher) {
            try {
                _uiState.update { it.copy(progressMessage = "Discovering candidates...") }
                val useCase = buildUseCase(apiKeys)
                val state = _uiState.value
                val customTickerList = state.customTickers.map { it.symbol }
                
                Logger.event("analysis_input", mapOf(
                    "custom_tickers_count" to customTickerList.size,
                    "provider_screener_enabled" to (state.hasAnyApiKeys)
                ))

                _uiState.update { it.copy(progressMessage = "Filtering tradeable symbols...") }
                val result = useCase.execute(
                    intent = state.intent,
                    risk = state.appSettings.riskTolerance,
                    assetClass = state.assetClass,
                    discoveryMode = state.appSettings.discoveryMode,
                    customTickers = customTickerList,
                    blocklist = state.blocklist,
                    screenerThresholds = mapOf(
                        "conservative" to state.appSettings.screenerConservativeThreshold,
                        "moderate" to state.appSettings.screenerModerateThreshold,
                        "aggressive" to state.appSettings.screenerAggressiveThreshold
                    )
                )
                _uiState.update { it.copy(isLoading = false, progressMessage = null) }
                Logger.event("analysis_completed", mapOf("setups" to result.setups.size))
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = result,
                        lastRunAt = result.generatedAt,
                        navigationEvent = NavigationEvent.Results
                    )
                }
                dbRepository.saveHistory(result)
                val resultsWithoutRemoved = result.setups.filter { !state.removedAlerts.contains(it.symbol) }
                val symbols = resultsWithoutRemoved.map { it.symbol }.distinct().filter { !state.blocklist.contains(it) }
                alertStore.saveSymbols(symbols)
                _uiState.update { it.copy(alertSymbolCount = symbols.size, alertSymbols = symbols) }

                // Notify for high-confidence signals
                result.setups.filter { it.confidence > 0.8 }.forEach { setup ->
                    com.polaralias.signalsynthesis.util.NotificationHelper.showTradeSignal(
                        context = application,
                        symbol = setup.symbol,
                        setupType = setup.setupType,
                        confidence = setup.confidence,
                        intent = setup.intent
                    )
                }

                // Trigger Prefetching
                val llmKey = _uiState.value.keys.llmKey
                if (llmKey.isNotBlank() && result.setups.isNotEmpty()) {
                    _uiState.update { it.copy(isPrefetching = true, prefetchCount = 0, progressMessage = "Prefetching AI insights...") }
                    
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
                    _uiState.update { it.copy(isPrefetching = false, progressMessage = null) }
                }
            } catch (ex: kotlinx.coroutines.CancellationException) {
                Logger.i("ViewModel", "Analysis cancelled")
                _uiState.update { it.copy(isLoading = false) }
            } catch (ex: Exception) {
                Logger.e("ViewModel", "Analysis failed", ex)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        progressMessage = null,
                        errorMessage = ex.message ?: "Analysis failed. Please try again."
                    )
                }
            } finally {
                analysisJob = null
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        _uiState.update { it.copy(isLoading = false, isPaused = false, progressMessage = null) }
        Logger.event("analysis_cancelled")
    }

    fun togglePause() {
        val currentlyPaused = _uiState.value.isPaused
        _uiState.update { it.copy(isPaused = !currentlyPaused) }
        viewModelScope.launch(ioDispatcher) {
            val settings = appSettingsStore.loadSettings()
            appSettingsStore.saveSettings(settings.copy(isAnalysisPaused = !currentlyPaused))
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
        
        // Find setup in current result or history
        val setup = state.result?.setups?.firstOrNull { it.symbol == symbol } 
            ?: state.history.flatMap { it.setups }.firstOrNull { it.symbol == symbol }
            ?: return
            
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

                _uiState.update { it.copy(progressMessage = "Synthesizing Setup...") }
                val useCase = buildSynthesisUseCase(state.keys.toApiKeys())
                val synthesis = useCase.execute(
                    setup = setup,
                    llmKey = llmKey,
                    reasoningDepth = state.appSettings.reasoningDepth,
                    outputLength = state.appSettings.outputLength,
                    verbosity = state.appSettings.verbosity,
                    onProgress = { msg ->
                        _uiState.update { it.copy(progressMessage = "AI Synthesis: $msg") }
                    }
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
                _uiState.update { it.copy(progressMessage = null) }
            } catch (ex: Exception) {
                _uiState.update { it.copy(progressMessage = null) }
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
        val setup = state.result?.setups?.firstOrNull { it.symbol == symbol }
            ?: state.history.flatMap { it.setups }.firstOrNull { it.symbol == symbol }
            ?: return
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

                val settings = _uiState.value.appSettings
                val client = llmClientFactory.create(getSuggestionModel())
                
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
                val systemPrompt = com.polaralias.signalsynthesis.domain.ai.AiPrompts.SCREENER_SUGGESTION_SYSTEM.trimIndent()

                val settings = _uiState.value.appSettings
                val client = llmClientFactory.create(getSuggestionModel())
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
                screenerAggressiveThreshold = suggestion.aggressiveLimit,
                screenerMinVolume = suggestion.minVolume
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

    fun addToBlocklist(symbol: String) {
        val upper = symbol.trim().uppercase()
        if (upper.isBlank()) return
        val current = _uiState.value.blocklist
        if (current.contains(upper)) return

        val newList = current + upper
        _uiState.update { it.copy(blocklist = newList) }
        saveBlocklist(newList)
        
        // Also remove from current alerts if present
        if (_uiState.value.alertSymbols.contains(upper)) {
            val newAlerts = _uiState.value.alertSymbols.filter { it != upper }
            _uiState.update { it.copy(alertSymbols = newAlerts, alertSymbolCount = newAlerts.size) }
            viewModelScope.launch(ioDispatcher) {
                alertStore.saveSymbols(newAlerts)
            }
        }
    }

    fun removeFromBlocklist(symbol: String) {
        val newList = _uiState.value.blocklist.filter { it != symbol }
        _uiState.update { it.copy(blocklist = newList) }
        saveBlocklist(newList)
    }

    private fun saveBlocklist(blocklist: List<String>) {
        viewModelScope.launch(ioDispatcher) {
            (appSettingsStore as? AppSettingsStore)?.saveBlocklist(blocklist)
        }
    }

    fun removeAlert(symbol: String) {
        val currentRemoved = _uiState.value.removedAlerts
        val newRemoved = currentRemoved + symbol
        _uiState.update { state -> 
            val newAlerts = state.alertSymbols.filter { it != symbol }
            state.copy(
                removedAlerts = newRemoved,
                alertSymbols = newAlerts,
                alertSymbolCount = newAlerts.size
            )
        }
        viewModelScope.launch(ioDispatcher) {
            alertStore.saveSymbols(_uiState.value.alertSymbols)
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
                _uiState.update { state ->
                    state.copy(
                        history = list,
                        // If no current result, load the most recent one from history
                        result = state.result ?: list.firstOrNull()
                    )
                }
            }
        }
    }
    
    fun clearNavigation() {
        _uiState.update { it.copy(navigationEvent = null) }
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
                    alertSymbolCount = symbols.size,
                    alertSymbols = symbols
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
                
                // 1. Fetch main indices
                val indexSymbols = listOf("SPY", "QQQ", "DIA", "IWM", "VIX")
                val indexQuotes = repository.getQuotes(indexSymbols)
                
                val indexItems = indexSymbols.mapNotNull { symbol ->
                    val quote = indexQuotes[symbol] ?: return@mapNotNull null
                    val names = mapOf(
                        "SPY" to "S&P 500",
                        "QQQ" to "Nasdaq 100",
                        "DIA" to "Dow 30",
                        "IWM" to "Russell 2000",
                        "VIX" to "Volatility"
                    )
                    IndexQuote(
                        symbol = symbol,
                        name = names[symbol] ?: symbol,
                        price = quote.price,
                        changePercent = quote.changePercent ?: 0.0,
                        volume = quote.volume
                    )
                }

                val sections = mutableListOf<MarketSection>()
                if (indexItems.isNotEmpty()) {
                    sections.add(MarketSection("Indices", indexItems))
                }

                // 2. Fetch Gainers
                try {
                    val gainerSymbols = repository.getTopGainers(5)
                    if (gainerSymbols.isNotEmpty()) {
                        val gainerQuotes = repository.getQuotes(gainerSymbols)
                        val gainerItems = gainerSymbols.mapNotNull { symbol ->
                            val quote = gainerQuotes[symbol] ?: return@mapNotNull null
                            IndexQuote(
                                symbol = symbol,
                                name = symbol,
                                price = quote.price,
                                changePercent = quote.changePercent ?: 0.0,
                                volume = quote.volume
                            )
                        }
                        if (gainerItems.isNotEmpty()) {
                            sections.add(MarketSection("Top Gainers", gainerItems))
                        }
                    }
                } catch (e: Exception) {
                    Logger.e("ViewModel", "Failed to fetch gainers", e)
                }

                // 3. Fetch Losers
                try {
                    val loserSymbols = repository.getTopLosers(5)
                    if (loserSymbols.isNotEmpty()) {
                        val loserQuotes = repository.getQuotes(loserSymbols)
                        val loserItems = loserSymbols.mapNotNull { symbol ->
                            val quote = loserQuotes[symbol] ?: return@mapNotNull null
                            IndexQuote(
                                symbol = symbol,
                                name = symbol,
                                price = quote.price,
                                changePercent = quote.changePercent ?: 0.0,
                                volume = quote.volume
                            )
                        }
                        if (loserItems.isNotEmpty()) {
                            sections.add(MarketSection("Top Losers", loserItems))
                        }
                    }
                } catch (e: Exception) {
                    Logger.e("ViewModel", "Failed to fetch losers", e)
                }

                if (sections.isNotEmpty()) {
                    _uiState.update { 
                        it.copy(
                            marketOverview = MarketOverview(sections, clock.instant()),
                            isLoadingMarket = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingMarket = false) }
                }
            } catch (e: Exception) {
                Logger.e("ViewModel", "Market overview refresh failed", e)
                _uiState.update { it.copy(isLoadingMarket = false) }
            }
        }
    }

    private fun buildSynthesisUseCase(
        apiKeys: com.polaralias.signalsynthesis.data.provider.ApiKeys
    ): SynthesizeSetupUseCase {
        val settings = _uiState.value.appSettings
        val bundle = providerFactory.build(apiKeys)
        val repository = MarketDataRepository(bundle)
        
        val analysisModel = if (settings.reasoningDepth == com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.DEEP || 
                                settings.reasoningDepth == com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.EXTRA) {
            settings.reasoningModel
        } else {
            settings.analysisModel
        }
        
        val analysisClient = llmClientFactory.create(analysisModel)
        val verdictClient = llmClientFactory.create(settings.verdictModel)
        
        return SynthesizeSetupUseCase(repository, analysisClient, verdictClient)
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
            val custom: List<String> = (appSettingsStore as? AppSettingsStore)?.loadCustomTickers() ?: emptyList()
            val blocklist = (appSettingsStore as? AppSettingsStore)?.loadBlocklist() ?: emptyList()
            _uiState.update { state ->
                state.copy(
                    appSettings = settings,
                    customTickers = custom.map { ticker -> TickerEntry(ticker) },
                    blocklist = blocklist,
                    isPaused = settings.isAnalysisPaused
                ) 
            }
        }
    }

    private fun observeDailyUsage() {
        viewModelScope.launch {
            com.polaralias.signalsynthesis.util.UsageTracker.dailyApiCount.collect { count ->
                _uiState.update { it.copy(dailyApiUsage = count) }
            }
        }
        viewModelScope.launch {
            com.polaralias.signalsynthesis.util.UsageTracker.dailyProviderUsage.collect { usage ->
                _uiState.update { it.copy(dailyProviderUsage = usage) }
            }
        }
        viewModelScope.launch {
            com.polaralias.signalsynthesis.util.UsageTracker.archivedUsage.collect { archived ->
                _uiState.update { it.copy(archivedUsage = archived) }
            }
        }
    }

    fun archiveUsage() {
        com.polaralias.signalsynthesis.util.UsageTracker.manualArchive()
    }

    private fun observeProviderBlacklist() {
        viewModelScope.launch {
            com.polaralias.signalsynthesis.data.provider.ProviderStatusManager.blacklist.collect { blacklist ->
                val now = System.currentTimeMillis()
                val activeBlacklist = blacklist.filterValues { it > now }.keys
                    .map { simplifyProviderName(it) }
                    .toList()
                _uiState.update { it.copy(blacklistedProviders = activeBlacklist) }
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
            return adapter.fromJson(validJson) ?: AiScreenerSuggestion(5.0, 20.0, 100.0, 1000000L, "Could not parse suggestion.")
        } catch (e: Exception) {
            Logger.e("AnalysisViewModel", "Failed to parse AI screener suggestion", e)
            return AiScreenerSuggestion(5.0, 20.0, 100.0, 1000000L, "Failed to parse AI suggestion.")
        }
    }

    private fun getSuggestionModel(): com.polaralias.signalsynthesis.domain.ai.LlmModel {
        val provider = _uiState.value.appSettings.llmProvider
        return if (provider == com.polaralias.signalsynthesis.domain.ai.LlmProvider.OPENAI) {
            com.polaralias.signalsynthesis.domain.ai.LlmModel.GPT_5_MINI
        } else {
            com.polaralias.signalsynthesis.domain.ai.LlmModel.GEMINI_3_FLASH
        }
    }

    private fun simplifyProviderName(name: String): String {
        return name.replace("MarketDataProvider", "").replace("DataProvider", "")
    }
}

enum class KeyField {
    ALPACA_KEY,
    ALPACA_SECRET,
    MASSIVE,
    FINNHUB,
    FMP,
    TWELVE_DATA,
    LLM
}
