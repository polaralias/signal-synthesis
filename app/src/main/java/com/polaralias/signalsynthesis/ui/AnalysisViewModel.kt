package com.polaralias.signalsynthesis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.signalsynthesis.data.provider.MarketDataProviderFactory
import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.provider.ApiKeys
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStorage
import com.polaralias.signalsynthesis.data.repository.AiSummaryRepository
import com.polaralias.signalsynthesis.data.repository.DatabaseRepository
import com.polaralias.signalsynthesis.data.cache.CacheTtlConfig
import com.polaralias.signalsynthesis.data.alerts.AlertTarget
import com.polaralias.signalsynthesis.data.alerts.AlertDirection
import com.polaralias.signalsynthesis.data.settings.AppSettings
import com.polaralias.signalsynthesis.data.storage.ApiKeyStorage
import com.polaralias.signalsynthesis.data.storage.AppSettingsStorage
import com.polaralias.signalsynthesis.data.storage.AppSettingsStore
import com.polaralias.signalsynthesis.data.worker.WorkScheduler
import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.usecase.PrefetchAiSummariesUseCase
import com.polaralias.signalsynthesis.domain.usecase.RunAnalysisUseCase
import com.polaralias.signalsynthesis.domain.usecase.RunAnalysisV2UseCase
import com.polaralias.signalsynthesis.domain.usecase.SynthesizeSetupUseCase
import com.polaralias.signalsynthesis.domain.usecase.BuildRssDigestUseCase
import com.polaralias.signalsynthesis.domain.usecase.ShortlistCandidatesUseCase
import com.polaralias.signalsynthesis.data.rss.RssDao
import com.polaralias.signalsynthesis.data.rss.RssFeedClient
import com.polaralias.signalsynthesis.data.rss.RssFeedCatalogLoader
import com.polaralias.signalsynthesis.data.rss.RssFeedDefaults
import com.polaralias.signalsynthesis.util.Logger
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.IndexQuote
import com.polaralias.signalsynthesis.domain.model.MarketOverview
import com.polaralias.signalsynthesis.domain.model.MarketSection
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.domain.rss.RssFeedResolver
import com.polaralias.signalsynthesis.domain.rss.RssFeedSelection
import com.polaralias.signalsynthesis.domain.rss.RssFeedStage
import com.polaralias.signalsynthesis.domain.rss.RssTickerInput
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import java.security.MessageDigest

class AnalysisViewModel(
    private val providerFactory: MarketDataProviderFactory,
    private val keyStore: ApiKeyStorage,
    private val alertStore: AlertSettingsStorage,
    private val workScheduler: WorkScheduler,
    private val dbRepository: DatabaseRepository,
    private val appSettingsStore: AppSettingsStorage,
    private val aiSummaryRepository: AiSummaryRepository,
    private val rssDao: RssDao,
    private val application: android.app.Application,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var analysisJob: kotlinx.coroutines.Job? = null

    private data class RepositoryConfig(
        val apiKeys: ApiKeys,
        val cacheConfig: CacheTtlConfig,
        val useMock: Boolean
    )

    private var cachedRepository: MarketDataRepository? = null
    private var cachedRepositoryConfig: RepositoryConfig? = null

    private val openAiResponsesService by lazy { com.polaralias.signalsynthesis.data.ai.OpenAiResponsesService.create() }
    private val openAiService by lazy { com.polaralias.signalsynthesis.data.ai.OpenAiService.create() }
    private val geminiService by lazy { com.polaralias.signalsynthesis.data.ai.GeminiService.create() }

    private val stageModelRouter by lazy {
            com.polaralias.signalsynthesis.domain.ai.StageModelRouter(
                openAiRunnerFactory = { model, key ->
                    com.polaralias.signalsynthesis.data.ai.OpenAiStageRunner(openAiService, openAiResponsesService, model, key)
                },
                geminiRunnerFactory = { model, key ->
                    com.polaralias.signalsynthesis.data.ai.GeminiStageRunner(geminiService, model, key)
                },
                routingConfigProvider = { effectiveRouting(_uiState.value.appSettings) },
                apiKeysProvider = {
                    mapOf(
                        com.polaralias.signalsynthesis.domain.ai.LlmProvider.OPENAI to _uiState.value.keys.openAiKey,
                        com.polaralias.signalsynthesis.domain.ai.LlmProvider.GEMINI to _uiState.value.keys.geminiKey
                    )
                }
            )
    }

    private val deepDiveUseCase by lazy {
        val openAiProvider = com.polaralias.signalsynthesis.data.ai.OpenAiDeepDiveProvider(openAiResponsesService)
        val geminiProvider = com.polaralias.signalsynthesis.data.ai.GeminiDeepDiveProvider(geminiService)
        com.polaralias.signalsynthesis.domain.usecase.DeepDiveUseCase(
            stageModelRouter = stageModelRouter,
            openAiProvider = openAiProvider,
            geminiProvider = geminiProvider,
            routingConfigProvider = { effectiveRouting(_uiState.value.appSettings) },
            apiKeysProvider = {
                mapOf(
                    com.polaralias.signalsynthesis.domain.ai.LlmProvider.OPENAI to _uiState.value.keys.openAiKey,
                    com.polaralias.signalsynthesis.domain.ai.LlmProvider.GEMINI to _uiState.value.keys.geminiKey
                )
            }
        )
    }

    private val rssCatalog by lazy { RssFeedCatalogLoader(application).load() }
    private val rssFeedResolver = RssFeedResolver()

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
                KeyField.OPENAI -> state.keys.copy(openAiKey = value)
                KeyField.GEMINI -> state.keys.copy(geminiKey = value)
            }
            state.copy(keys = keys)
        }
    }

    fun saveKeys() {
        viewModelScope.launch(ioDispatcher) {
            val keys = _uiState.value.keys
            keyStore.saveKeys(keys.toApiKeys(), keys.toLlmKeys())
            Logger.event("keys_saved", mapOf("has_llm_key" to (keys.openAiKey.isNotBlank() || keys.geminiKey.isNotBlank())))
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
        val uiStateValue = _uiState.value
        val apiKeys = uiStateValue.keys.toApiKeys()
        val llmKey = uiStateValue.keys.openAiKey.ifBlank { uiStateValue.keys.geminiKey }
        
        if (!apiKeys.hasAny() && !uiStateValue.appSettings.useMockDataWhenOffline) {
            _uiState.update { it.copy(errorMessage = "Add at least one provider key or enable mock data mode in settings.") }
            return
        }

        val useStaged = uiStateValue.appSettings.useStagedPipeline
        if (useStaged) {
            val missingProviders = missingLlmProvidersForStages(
                listOf(
                    com.polaralias.signalsynthesis.domain.model.AnalysisStage.SHORTLIST,
                    com.polaralias.signalsynthesis.domain.model.AnalysisStage.DECISION_UPDATE,
                    com.polaralias.signalsynthesis.domain.model.AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS
                )
            )
            if (missingProviders.isNotEmpty()) {
                val providersLabel = missingProviders.joinToString { it.name }
                _uiState.update { it.copy(errorMessage = "Missing API key(s) for: $providersLabel") }
                return
            }
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, aiSummaries = emptyMap(), isPaused = false, removedAlerts = emptySet()) }
        Logger.event("analysis_started", mapOf("intent" to uiStateValue.intent.name, "useStaged" to useStaged))
        
        analysisJob = viewModelScope.launch(ioDispatcher) {
            try {
                val state = _uiState.value
                val customTickerList = state.customTickers.map { it.symbol }
                val repository = getRepository(apiKeys)

                val result = if (useStaged) {
                    val rssClient = RssFeedClient(rssDao = rssDao)
                    val rssDigestBuilder = BuildRssDigestUseCase(rssClient, rssDao)
                    val useCase = RunAnalysisV2UseCase(repository, stageModelRouter, rssDigestBuilder, clock)
                    useCase.execute(
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
                        ),
                        rssSelection = rssSelectionFrom(state.appSettings),
                        rssCatalog = rssCatalog,
                        onProgress = { msg ->
                            _uiState.update { it.copy(progressMessage = msg) }
                        }
                    )
                } else {
                    _uiState.update { it.copy(progressMessage = "Discovering candidates...") }
                    val useCase = RunAnalysisUseCase(repository, clock)
                    useCase.execute(
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
                }

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
                val targets = resultsWithoutRemoved
                    .filter { symbols.contains(it.symbol) }
                    .map { setup ->
                        val direction = if (setup.targetPrice >= setup.triggerPrice) AlertDirection.ABOVE else AlertDirection.BELOW
                        AlertTarget(
                            symbol = setup.symbol,
                            targetPrice = setup.targetPrice,
                            direction = direction
                        )
                    }
                alertStore.saveTargets(targets)
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
                if (state.appSettings.aiSummaryPrefetchEnabled && llmKey.isNotBlank() && result.setups.isNotEmpty()) {
                    _uiState.update { it.copy(isPrefetching = true, prefetchCount = 0, progressMessage = "Prefetching AI insights...") }
                    
                    val prefetchFlow = PrefetchAiSummariesUseCase(
                        buildSynthesisUseCase(apiKeys),
                        aiSummaryRepository
                    ).execute(
                        setups = result.setups,
                        llmKey = llmKey,
                        maxPrefetch = state.appSettings.aiSummaryPrefetchLimit,
                        cacheKeyProvider = { setup -> buildAiSummaryCacheKey(setup, state.appSettings) }
                    )

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
        val llmKey = state.keys.openAiKey.ifBlank { state.keys.geminiKey }
        if (llmKey.isBlank() || !state.hasLlmKey) return
        val missingProviders = missingLlmProvidersForStages(
            listOf(
                com.polaralias.signalsynthesis.domain.model.AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS,
                com.polaralias.signalsynthesis.domain.model.AnalysisStage.DECISION_UPDATE
            )
        )
        if (missingProviders.isNotEmpty()) {
            _uiState.update { it.copy(errorMessage = "Missing API key(s) for: ${missingProviders.joinToString { provider -> provider.name }}") }
            return
        }
        
        // Find setup in current result or history
        val setup = state.result?.setups?.firstOrNull { it.symbol == symbol } 
            ?: state.history.flatMap { it.setups }.firstOrNull { it.symbol == symbol }
            ?: return
        val cacheKey = buildAiSummaryCacheKey(setup, state.appSettings)
            
        val existing = state.aiSummaries[symbol]
        if (existing?.status == AiSummaryStatus.LOADING || existing?.status == AiSummaryStatus.READY) return

        updateAiSummary(symbol, AiSummaryState(status = AiSummaryStatus.LOADING))
        viewModelScope.launch(ioDispatcher) {
            try {
                // Check cache first
                val cached = aiSummaryRepository.getSummary(symbol, cacheKey.model, cacheKey.promptHash)
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
                aiSummaryRepository.saveSummary(symbol, cacheKey.model, cacheKey.promptHash, synthesis)

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
                val repository = getRepository(apiKeys)

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

    fun requestDeepDive(symbol: String) {
        val state = _uiState.value
        val llmKey = state.keys.openAiKey.ifBlank { state.keys.geminiKey }
        if (llmKey.isBlank() || !state.hasLlmKey) return
        
        val setup = state.result?.setups?.firstOrNull { it.symbol == symbol } 
            ?: state.history.flatMap { it.setups }.firstOrNull { it.symbol == symbol }
            ?: return
            
        val existing = state.deepDives[symbol]
        if (existing?.status == DeepDiveStatus.LOADING || existing?.status == DeepDiveStatus.READY) return

        updateDeepDive(symbol, DeepDiveState(status = DeepDiveStatus.LOADING))
        
        viewModelScope.launch(ioDispatcher) {
            try {
                // Fetch recent RSS headlines for this ticker to pass them to LLM
                val rssClient = com.polaralias.signalsynthesis.data.rss.RssFeedClient(rssDao = rssDao)
                val rssDigestBuilder = com.polaralias.signalsynthesis.domain.usecase.BuildRssDigestUseCase(rssClient, rssDao)
                val rssSelection = rssSelectionFrom(state.appSettings)
                val resolvedFeeds = rssFeedResolver.resolve(
                    catalog = rssCatalog,
                    selection = rssSelection,
                    tickers = listOf(
                        RssTickerInput(
                            symbol = symbol,
                            source = setup.source,
                            rssNeeded = setup.rssNeeded,
                            expandedRssNeeded = setup.expandedRssNeeded
                        )
                    ),
                    stage = RssFeedStage.DEEP_DIVE
                )
                val rssDigestResponse = if (resolvedFeeds.feedUrls.isNotEmpty()) {
                    rssDigestBuilder.execute(
                        tickers = listOf(symbol),
                        feedUrls = resolvedFeeds.feedUrls,
                        timeWindowHours = 72
                    )
                } else null
                val rssDigest = rssDigestResponse?.itemsBySymbol?.get(symbol)

                // Create a data snapshot
                val snapshot = "Technical: ${setup.intradayStats?.rsi14 ?: "N/A"} RSI, ${setup.intradayStats?.vwap ?: "N/A"} VWAP. " +
                        "Fundamental: ${setup.profile?.name ?: "N/A"}, PE: ${setup.metrics?.peRatio ?: "N/A"}."

                val deepDive = deepDiveUseCase.execute(
                    symbol = symbol,
                    intent = setup.intent,
                    snapshot = snapshot,
                    rssHeadlinesList = rssDigest
                )
                
                updateDeepDive(symbol, DeepDiveState(status = DeepDiveStatus.READY, data = deepDive))
            } catch (ex: Exception) {
                Logger.e("ViewModel", "Deep dive failed for $symbol", ex)
                updateDeepDive(symbol, DeepDiveState(status = DeepDiveStatus.ERROR, errorMessage = ex.message ?: "Deep dive failed."))
            }
        }
    }

    private fun updateDeepDive(symbol: String, state: DeepDiveState) {
        _uiState.update { 
            val newMap = it.deepDives.toMutableMap()
            newMap[symbol] = state
            it.copy(deepDives = newMap)
        }
    }

    fun suggestThresholdsWithAi(prompt: String) {
        val llmKey = _uiState.value.keys.openAiKey.ifBlank { _uiState.value.keys.geminiKey }
        if (llmKey.isBlank()) return

        _uiState.update { it.copy(isSuggestingThresholds = true) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val systemPrompt = com.polaralias.signalsynthesis.domain.ai.AiPrompts.THRESHOLD_SUGGESTION_SYSTEM.trimIndent()

                val request = com.polaralias.signalsynthesis.domain.ai.LlmStageRequest(
                    systemPrompt = systemPrompt,
                    userPrompt = "User context: $prompt",
                    stage = com.polaralias.signalsynthesis.domain.model.AnalysisStage.DECISION_UPDATE,
                    expectedSchemaId = "AiThresholdSuggestion"
                )
                
                val startTime = System.currentTimeMillis()
                val response = stageModelRouter.run(com.polaralias.signalsynthesis.domain.model.AnalysisStage.DECISION_UPDATE, request)
                val duration = System.currentTimeMillis() - startTime
                com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("ThresholdSuggestion", prompt, response.rawText, true, duration)

                val suggestion = parseAiThresholdSuggestion(response.rawText)
                _uiState.update { it.copy(aiThresholdSuggestion = suggestion, isSuggestingThresholds = false) }
            } catch (ex: Exception) {
                com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("ThresholdSuggestion", prompt, ex.message ?: "Failed", false, 0)
                _uiState.update { it.copy(isSuggestingThresholds = false, errorMessage = "AI suggestion failed: ${ex.message}") }
            }
        }
    }

    fun suggestScreenerWithAi(prompt: String) {
        val llmKey = _uiState.value.keys.openAiKey.ifBlank { _uiState.value.keys.geminiKey }
        if (llmKey.isBlank()) return

        _uiState.update { it.copy(isSuggestingScreener = true) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val systemPrompt = com.polaralias.signalsynthesis.domain.ai.AiPrompts.SCREENER_SUGGESTION_SYSTEM.trimIndent()

                val request = com.polaralias.signalsynthesis.domain.ai.LlmStageRequest(
                    systemPrompt = systemPrompt,
                    userPrompt = "User context: $prompt",
                    stage = com.polaralias.signalsynthesis.domain.model.AnalysisStage.DECISION_UPDATE,
                    expectedSchemaId = "AiScreenerSuggestion"
                )
                val startTime = System.currentTimeMillis()
                val response = stageModelRouter.run(com.polaralias.signalsynthesis.domain.model.AnalysisStage.DECISION_UPDATE, request)
                val duration = System.currentTimeMillis() - startTime
                com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("ScreenerSuggestion", prompt, response.rawText, true, duration)

                val suggestion = parseAiScreenerSuggestion(response.rawText)
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
            com.polaralias.signalsynthesis.util.ActivityLogger.setVerboseLogging(settings.verboseLogging)
            
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

    fun updateStageConfig(stage: com.polaralias.signalsynthesis.domain.model.AnalysisStage, config: com.polaralias.signalsynthesis.domain.ai.StageModelConfig) {
        val currentRouting = _uiState.value.appSettings.modelRouting
        val newMap = currentRouting.byStage.toMutableMap()
        newMap[stage] = config
        val updatedSettings = _uiState.value.appSettings.copy(
            modelRouting = currentRouting.copy(byStage = newMap)
        )
        updateAppSettings(updatedSettings)
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
                val repository = getRepository(apiKeys)
                val results = repository.searchSymbols(query)
                _uiState.update { it.copy(tickerSearchResults = results, isSearchingTickers = false) }
            } catch (e: Exception) {
                Logger.e("AnalysisViewModel", "Ticker search failed", e)
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

    fun toggleRssTopic(topicKey: String) {
        val current = _uiState.value.appSettings.rssEnabledTopics
        val updated = if (current.contains(topicKey)) current - topicKey else current + topicKey
        updateAppSettings(_uiState.value.appSettings.copy(rssEnabledTopics = updated))
    }

    fun toggleRssTickerSource(sourceId: String) {
        val current = _uiState.value.appSettings.rssTickerSources
        val updated = if (current.contains(sourceId)) current - sourceId else current + sourceId
        updateAppSettings(_uiState.value.appSettings.copy(rssTickerSources = updated))
    }

    fun updateRssUseTickerFeedsForFinalStage(enabled: Boolean) {
        updateAppSettings(_uiState.value.appSettings.copy(rssUseTickerFeedsForFinalStage = enabled))
    }

    fun updateRssApplyExpandedToAll(enabled: Boolean) {
        updateAppSettings(_uiState.value.appSettings.copy(rssApplyExpandedToAll = enabled))
    }

    fun resetRssDefaults() {
        updateAppSettings(
            _uiState.value.appSettings.copy(
                rssEnabledTopics = RssFeedDefaults.defaultEnabledTopicKeys(),
                rssTickerSources = RssFeedDefaults.defaultTickerSourceIds,
                rssUseTickerFeedsForFinalStage = true,
                rssApplyExpandedToAll = false
            )
        )
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
                val updatedTargets = alertStore.loadTargets().filterNot { it.symbol.equals(upper, ignoreCase = true) }
                alertStore.saveTargets(updatedTargets)
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
            val updatedTargets = alertStore.loadTargets().filterNot { it.symbol.equals(symbol, ignoreCase = true) }
            alertStore.saveTargets(updatedTargets)
        }
    }

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch(ioDispatcher) {
            val current = _uiState.value.watchlist
            if (current.contains(symbol)) {
                dbRepository.removeFromWatchlist(symbol)
            } else {
                val setupIntent = _uiState.value.result?.setups?.firstOrNull { it.symbol == symbol }?.intent
                    ?: _uiState.value.history.flatMap { it.setups }.firstOrNull { it.symbol == symbol }?.intent
                    ?: _uiState.value.intent
                dbRepository.addToWatchlist(symbol, setupIntent)
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
            val llmKeys = keyStore.loadLlmKeys()
            _uiState.update {
                it.copy(
                    keys = ApiKeyUiState.from(apiKeys, llmKeys),
                    hasAnyApiKeys = apiKeys.hasAny(),
                    hasLlmKey = !llmKeys.openAiKey.isNullOrBlank() || !llmKeys.geminiKey.isNullOrBlank()
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
        val repository = getRepository(apiKeys)
        return RunAnalysisUseCase(repository, clock)
    }

    fun refreshMarketOverview() {
        val state = _uiState.value
        if (state.isLoadingMarket) return

        _uiState.update { it.copy(isLoadingMarket = true) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val apiKeys = _uiState.value.keys.toApiKeys()
                val repository = getRepository(apiKeys)
                
                // 1. Fetch main indices
                // User requested: S&P 500, Gold, USD/GBP
                val indexSymbols = listOf("SPY", "GLD", "GBPUSD")
                
                // If we have API keys, do NOT fallback to mock data silently.
                // The repository might handle this, but let's ensure we are asking for real data.
                val indexQuotes = if (apiKeys.hasAny()) {
                     repository.getQuotes(indexSymbols)
                } else {
                     // In demo mode, repository uses mock provider automatically if configured
                     repository.getQuotes(indexSymbols)
                }
                
                val indexItems = indexSymbols.mapNotNull { symbol ->
                    val quote = indexQuotes[symbol]
                    // If no quote and we have keys, it means data unavailable -> show error or blank?
                    // User said: "if unavailable be blank or show an accurate state message"
                    // mapNotNull will allow us to skip it if null.
                    // If all are null, we might show "Market Data Unavailable"
                    
                    if (quote == null) return@mapNotNull null
                    
                    val names = mapOf(
                        "SPY" to "S&P 500",
                        "GLD" to "Gold",
                        "GBPUSD" to "USD/GBP"
                    )
                    IndexQuote(
                        symbol = symbol,
                        name = names[symbol] ?: symbol,
                        price = quote.price,
                        changePercent = quote.changePercent ?: 0.0,
                        volume = quote.volume
                    )
                }
                
                // If items are empty and we have keys, it means failure.
                if (indexItems.isEmpty() && apiKeys.hasAny()) {
                     // We could add a "State Message" item or just leave it empty.
                     // The UI handles empty sections by not showing them.
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
        val repository = getRepository(apiKeys)
        return SynthesizeSetupUseCase(repository, stageModelRouter)
    }

    private fun getRepository(apiKeys: ApiKeys): MarketDataRepository {
        val settings = _uiState.value.appSettings
        val cacheConfig = CacheTtlConfig.fromMinutes(
            quoteMinutes = settings.cacheTtlQuotesMinutes,
            intradayMinutes = settings.cacheTtlIntradayMinutes,
            dailyMinutes = settings.cacheTtlDailyMinutes,
            profileMinutes = settings.cacheTtlProfileMinutes,
            metricsMinutes = settings.cacheTtlMetricsMinutes,
            sentimentMinutes = settings.cacheTtlSentimentMinutes
        )
        val config = RepositoryConfig(
            apiKeys = apiKeys,
            cacheConfig = cacheConfig,
            useMock = settings.useMockDataWhenOffline
        )
        if (cachedRepository == null || cachedRepositoryConfig != config) {
            val bundle = if (!apiKeys.hasAny() && !settings.useMockDataWhenOffline) {
                ProviderBundle.empty()
            } else {
                providerFactory.build(apiKeys)
            }
            cachedRepository = MarketDataRepository(bundle, cacheConfig)
            cachedRepositoryConfig = config
        }
        return cachedRepository!!
    }

    private fun buildAiSummaryCacheKey(
        setup: com.polaralias.signalsynthesis.domain.model.TradeSetup,
        settings: AppSettings
    ): com.polaralias.signalsynthesis.domain.model.AiSummaryCacheKey {
        val routing = effectiveRouting(settings)
        val analysisModel = routing.getConfigForStage(com.polaralias.signalsynthesis.domain.model.AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS).model
        val verdictModel = routing.getConfigForStage(com.polaralias.signalsynthesis.domain.model.AnalysisStage.DECISION_UPDATE).model
        val model = "$analysisModel|$verdictModel"

        val raw = buildString {
            append(setup.symbol)
            append("|")
            append(setup.setupType)
            append("|")
            append(setup.triggerPrice)
            append("|")
            append(setup.stopLoss)
            append("|")
            append(setup.targetPrice)
            append("|")
            append(setup.intent.name)
            append("|")
            append(setup.reasons.joinToString(";"))
            append("|")
            append(setup.intradayStats?.rsi14 ?: "")
            append("|")
            append(setup.intradayStats?.vwap ?: "")
            append("|")
            append(setup.intradayStats?.atr14 ?: "")
            append("|")
            append(setup.eodStats?.sma50 ?: "")
            append("|")
            append(setup.eodStats?.sma200 ?: "")
            append("|")
            append(settings.reasoningDepth.name)
            append("|")
            append(settings.outputLength.name)
            append("|")
            append(settings.verbosity.name)
            append("|")
            append(model)
        }

        return com.polaralias.signalsynthesis.domain.model.AiSummaryCacheKey(
            model = model,
            promptHash = sha256(raw)
        )
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun effectiveRouting(settings: AppSettings): com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig {
        val baseRouting = settings.modelRouting
        val map = baseRouting.byStage.toMutableMap()

        val provider = settings.llmProvider
        val analysisModel = resolveModelForProvider(settings.analysisModel, provider)
        val verdictModel = resolveModelForProvider(settings.verdictModel, provider)
        val lengthTokens = tokensForLength(settings.outputLength)

        if (!map.containsKey(com.polaralias.signalsynthesis.domain.model.AnalysisStage.SHORTLIST)) {
            val base = baseRouting.getConfigForStage(com.polaralias.signalsynthesis.domain.model.AnalysisStage.SHORTLIST)
            map[com.polaralias.signalsynthesis.domain.model.AnalysisStage.SHORTLIST] = base.copy(
                provider = provider,
                model = analysisModel.modelId,
                reasoningDepth = settings.reasoningDepth
            )
        }

        if (!map.containsKey(com.polaralias.signalsynthesis.domain.model.AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS)) {
            val base = baseRouting.getConfigForStage(com.polaralias.signalsynthesis.domain.model.AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS)
            map[com.polaralias.signalsynthesis.domain.model.AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS] = base.copy(
                provider = provider,
                model = analysisModel.modelId,
                reasoningDepth = settings.reasoningDepth,
                maxOutputTokens = lengthTokens
            )
        }

        if (!map.containsKey(com.polaralias.signalsynthesis.domain.model.AnalysisStage.DECISION_UPDATE)) {
            val base = baseRouting.getConfigForStage(com.polaralias.signalsynthesis.domain.model.AnalysisStage.DECISION_UPDATE)
            map[com.polaralias.signalsynthesis.domain.model.AnalysisStage.DECISION_UPDATE] = base.copy(
                provider = provider,
                model = verdictModel.modelId,
                reasoningDepth = settings.reasoningDepth,
                maxOutputTokens = lengthTokens
            )
        }

        if (!map.containsKey(com.polaralias.signalsynthesis.domain.model.AnalysisStage.DEEP_DIVE)) {
            val deepProvider = settings.deepDiveProvider
            val deepModel = resolveModelForProvider(settings.reasoningModel, deepProvider)
            val base = baseRouting.getConfigForStage(com.polaralias.signalsynthesis.domain.model.AnalysisStage.DEEP_DIVE)
            map[com.polaralias.signalsynthesis.domain.model.AnalysisStage.DEEP_DIVE] = base.copy(
                provider = deepProvider,
                model = deepModel.modelId,
                reasoningDepth = settings.reasoningDepth
            )
        }

        return com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig(map)
    }

    private fun resolveModelForProvider(
        model: com.polaralias.signalsynthesis.domain.ai.LlmModel,
        provider: com.polaralias.signalsynthesis.domain.ai.LlmProvider
    ): com.polaralias.signalsynthesis.domain.ai.LlmModel {
        return if (model.provider == provider) {
            model
        } else {
            com.polaralias.signalsynthesis.domain.ai.LlmModel.values().first { it.provider == provider }
        }
    }

    private fun tokensForLength(length: com.polaralias.signalsynthesis.domain.ai.OutputLength): Int {
        return when (length) {
            com.polaralias.signalsynthesis.domain.ai.OutputLength.SHORT -> 400
            com.polaralias.signalsynthesis.domain.ai.OutputLength.STANDARD -> 800
            com.polaralias.signalsynthesis.domain.ai.OutputLength.FULL -> 1500
        }
    }

    private fun rssSelectionFrom(settings: AppSettings): RssFeedSelection {
        return RssFeedSelection(
            enabledTopicKeys = settings.rssEnabledTopics,
            enabledTickerSourceIds = settings.rssTickerSources,
            useTickerFeedsForFinalStage = settings.rssUseTickerFeedsForFinalStage,
            forceExpandedForAll = settings.rssApplyExpandedToAll
        )
    }

    fun clearCaches() {
        cachedRepository?.clearAllCaches()
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
            com.polaralias.signalsynthesis.util.ActivityLogger.setVerboseLogging(settings.verboseLogging)
            val custom: List<String> = (appSettingsStore as? AppSettingsStore)?.loadCustomTickers() ?: emptyList()
            val blocklist = (appSettingsStore as? AppSettingsStore)?.loadBlocklist() ?: emptyList()
            _uiState.update { state ->
                state.copy(
                    appSettings = settings,
                    customTickers = custom.map { ticker -> TickerEntry(ticker) },
                    blocklist = blocklist,
                    isPaused = settings.isAnalysisPaused,
                    rssCatalog = rssCatalog
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

    private fun missingLlmProvidersForStages(
        stages: List<com.polaralias.signalsynthesis.domain.model.AnalysisStage>
    ): List<com.polaralias.signalsynthesis.domain.ai.LlmProvider> {
        val routing = effectiveRouting(_uiState.value.appSettings)
        val keys = _uiState.value.keys
        return stages
            .map { routing.getConfigForStage(it).provider }
            .distinct()
            .filter { provider -> !hasKeyForProvider(provider, keys) }
    }

    private fun hasKeyForProvider(
        provider: com.polaralias.signalsynthesis.domain.ai.LlmProvider,
        keys: ApiKeyUiState
    ): Boolean {
        return when (provider) {
            com.polaralias.signalsynthesis.domain.ai.LlmProvider.OPENAI -> keys.openAiKey.isNotBlank()
            com.polaralias.signalsynthesis.domain.ai.LlmProvider.GEMINI -> keys.geminiKey.isNotBlank()
        }
    }


    private fun simplifyProviderName(name: String): String {
        return name.replace("MarketDataProvider", "").replace("DataProvider", "")
    }

    fun runShortlistHarness() {
        val uiStateValue = _uiState.value
        val llmKey = uiStateValue.keys.openAiKey.ifBlank { uiStateValue.keys.geminiKey }
        if (llmKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "LLM Key required for shortlist harness.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, progressMessage = "Running Shortlist Harness...") }
        viewModelScope.launch(ioDispatcher) {
            try {
                val apiKeys = uiStateValue.keys.toApiKeys()
                val repository = getRepository(apiKeys)
                
                // Fixed list for testing
                val testSymbols = listOf("AAPL", "TSLA", "NVDA", "AMD", "MSFT", "GOOGL", "AMZN", "META", "NFLX", "INTC")
                val quotes = repository.getQuotes(testSymbols)
                
                val shortlistUseCase = ShortlistCandidatesUseCase(stageModelRouter)
                
                val plan = shortlistUseCase.execute(
                    symbols = testSymbols,
                    quotes = quotes,
                    intent = uiStateValue.intent,
                    risk = uiStateValue.appSettings.riskTolerance,
                    maxShortlist = 5
                )
                
                Logger.i("ShortlistHarness", "Harness completed. Plan: $plan")
                _uiState.update { it.copy(isLoading = false, progressMessage = "Shortlist harness completed. Check logs.") }
            } catch (e: Exception) {
                Logger.e("ShortlistHarness", "Harness failed", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = "Shortlist harness failed: ${e.message}") }
            }
        }
    }

}

enum class KeyField {
    ALPACA_KEY,
    ALPACA_SECRET,
    MASSIVE,
    FINNHUB,
    FMP,
    TWELVE_DATA,
    OPENAI,
    GEMINI
}
