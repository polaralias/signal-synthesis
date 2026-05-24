package com.polaralias.signalsynthesis.ui

import com.polaralias.signalsynthesis.data.alerts.AlertSettings
import com.polaralias.signalsynthesis.data.provider.ApiKeys
import com.polaralias.signalsynthesis.data.provider.MarketDataProviderFactory
import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStorage
import com.polaralias.signalsynthesis.data.storage.ApiKeyStorage
import com.polaralias.signalsynthesis.data.worker.WorkScheduler
import com.polaralias.signalsynthesis.data.repository.DatabaseRepository
import com.polaralias.signalsynthesis.data.repository.AiSummaryRepository
import com.polaralias.signalsynthesis.data.rss.RssFeedCatalog
import com.polaralias.signalsynthesis.data.rss.RssFeedCatalogEntry
import com.polaralias.signalsynthesis.data.rss.RssFeedClient
import com.polaralias.signalsynthesis.data.rss.RssFeedStateEntity
import com.polaralias.signalsynthesis.data.rss.RssItemEntity
import com.polaralias.signalsynthesis.data.ai.ProviderModelDiscovery
import com.polaralias.signalsynthesis.data.settings.AppSettings
import com.polaralias.signalsynthesis.data.storage.AppSettingsStorage
import com.polaralias.signalsynthesis.data.storage.LlmKeys
import com.polaralias.signalsynthesis.data.settings.DiscoveryMode
import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.LlmModel
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.LlmStageResponse
import com.polaralias.signalsynthesis.domain.ai.StageModelRouter
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.DailyBar
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.IntradayBar
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.SentimentData
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.domain.provider.DailyProvider
import com.polaralias.signalsynthesis.domain.provider.IntradayProvider
import com.polaralias.signalsynthesis.domain.provider.MetricsProvider
import com.polaralias.signalsynthesis.domain.provider.ProfileProvider
import com.polaralias.signalsynthesis.domain.provider.QuoteProvider
import com.polaralias.signalsynthesis.domain.provider.SentimentProvider
import com.polaralias.signalsynthesis.testsupport.stageRouterFixture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AnalysisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsCorrect() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        // Allow init to complete
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        // hasAnyApiKeys depends on FakeApiKeyStore default which is false
        assertEquals(false, state.hasAnyApiKeys)
    }

    @Test
    fun updateKeyUpdatesState() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.updateKey(KeyField.ALPACA_KEY, "test_key")

        val state = viewModel.uiState.value
        assertEquals("test_key", state.keys.alpacaKey)
    }

    @Test
    fun runAnalysisFailsWithoutKeys() = runTest(testDispatcher) {
        val viewModel = createViewModel(hasKeys = false, mockWhenOffline = false)
        testDispatcher.scheduler.advanceUntilIdle() // let init finish

        viewModel.runAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Add at least one provider key") || state.errorMessage!!.contains("enable mock data"))
    }

    @Test
    fun runAnalysisUsesV2ByDefaultWhenLlmKeysExist() = runTest(testDispatcher) {
        val quotes = mapOf(
            "AAPL" to Quote("AAPL", 150.0, 10_000_000L, Instant.now(clock), changePercent = 1.2)
        )
        val routerFixture = stageRouterFixture(
            mapOf(
                AnalysisStage.SHORTLIST to jsonResponse(
                    """
                    {
                      "shortlist": [],
                      "global_notes": ["No qualified setups"]
                    }
                    """.trimIndent()
                )
            )
        )
        val viewModel = createViewModel(
            hasKeys = true,
            llmKeys = LlmKeys(openAiKey = "test-openai"),
            providerFactory = StaticProviderFactory(
                ProviderBundle(
                    quoteProviders = listOf(StaticQuoteProvider(quotes)),
                    intradayProviders = emptyList(),
                    dailyProviders = emptyList(),
                    profileProviders = emptyList(),
                    metricsProviders = emptyList(),
                    sentimentProviders = emptyList()
                )
            ),
            stageModelRouterOverride = routerFixture.router,
            appSettingsStore = FakeAppSettingsStore(
                AppSettings(
                    useMockDataWhenOffline = true,
                    discoveryMode = DiscoveryMode.CUSTOM
                )
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addCustomTicker("AAPL")
        viewModel.runAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.errorMessage)
        assertNotNull(state.result)
        assertEquals(listOf(AnalysisStage.SHORTLIST), routerFixture.calls.map { it.stage })
        assertEquals(listOf("No qualified setups"), state.result!!.globalNotes)
    }

    @Test
    fun runAnalysisFailsWhenDefaultPipelineIsMissingLlmKeys() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            hasKeys = true,
            llmKeys = LlmKeys(),
            appSettingsStore = FakeAppSettingsStore(
                AppSettings(
                    useMockDataWhenOffline = true
                )
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.runAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.result)
        assertTrue(state.errorMessage?.contains("Missing API key(s) for: OpenAI") == true)
    }

    @Test
    fun runAnalysisPublishesV2ArtifactsAndStageProgressInOrder() = runTest(testDispatcher) {
        val quotes = mapOf(
            "AAPL" to Quote("AAPL", 150.0, 10_000_000L, Instant.now(clock), changePercent = 1.2),
            "TSLA" to Quote("TSLA", 220.0, 12_000_000L, Instant.now(clock), changePercent = -0.4)
        )
        val providerFactory = StaticProviderFactory(
            ProviderBundle(
                quoteProviders = listOf(StaticQuoteProvider(quotes)),
                intradayProviders = listOf(
                    StaticIntradayProvider(mapOf("AAPL" to createIntradayBars(100.0)))
                ),
                dailyProviders = listOf(
                    StaticDailyProvider(mapOf("TSLA" to createDailyBars(200.0)))
                ),
                profileProviders = listOf(
                    StaticProfileProvider(
                        mapOf("AAPL" to CompanyProfile("Apple Inc.", "Technology", "Consumer Electronics", "Maker of phones"))
                    )
                ),
                metricsProviders = listOf(
                    StaticMetricsProvider(
                        mapOf("AAPL" to FinancialMetrics(2_000_000_000_000L, 30.0, 6.0, "2026-02-15"))
                    )
                ),
                sentimentProviders = listOf(
                    StaticSentimentProvider(mapOf("AAPL" to SentimentData(0.6, "Bullish")))
                )
            )
        )
        val routerFixture = stageRouterFixture(
            mapOf(
                AnalysisStage.SHORTLIST to jsonResponse(
                    """
                    {
                      "shortlist": [
                        {
                          "symbol": "AAPL",
                          "priority": 0.91,
                          "requested_enrichment": ["INTRADAY", "FUNDAMENTALS"]
                        },
                        {
                          "symbol": "TSLA",
                          "priority": 0.72,
                          "requested_enrichment": ["EOD"]
                        }
                      ],
                      "global_notes": ["Watch mega-cap leadership"]
                    }
                    """.trimIndent()
                ),
                AnalysisStage.DECISION_UPDATE to jsonResponse(
                    """
                    {
                      "keep": [
                        {
                          "symbol": "AAPL",
                          "confidence": 0.88,
                          "setup_bias": "long",
                          "must_review": ["earnings timing"],
                          "rss_needed": false,
                          "expanded_rss_needed": true,
                          "expanded_rss_reason": "Supply-chain checks"
                        }
                      ],
                      "drop": [
                        {
                          "symbol": "TSLA",
                          "reasons": ["weaker confirmation"]
                        }
                      ]
                    }
                    """.trimIndent()
                ),
                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS to jsonResponse(
                    """
                    {
                      "ranked_review_list": [
                        {
                          "symbol": "AAPL",
                          "what_to_review": ["Check supplier headlines"],
                          "risk_summary": ["Crowded long"],
                          "one_paragraph_brief": "Momentum remains constructive."
                        }
                      ],
                      "portfolio_guidance": {
                        "position_count": 1,
                        "risk_posture": "moderate"
                      }
                    }
                    """.trimIndent()
                )
            )
        )
        val rssDao = FakeRssDao(
            items = listOf(
                RssItemEntity(
                    guidHash = "aapl-1",
                    feedUrl = "http://yahoo/AAPL",
                    title = "AAPL supplier update",
                    link = "http://example.com/aapl",
                    publishedAt = System.currentTimeMillis(),
                    snippet = "AAPL gains on supplier update",
                    fetchedAt = System.currentTimeMillis()
                )
            )
        )
        val historyRepository = RecordingDatabaseRepository()
        val progressMessages = mutableListOf<String>()
        val viewModel = createViewModel(
            hasKeys = true,
            llmKeys = LlmKeys(openAiKey = "test-openai"),
            providerFactory = providerFactory,
            dbRepository = historyRepository,
            rssDao = rssDao,
            stageModelRouterOverride = routerFixture.router,
            rssCatalogOverride = RssFeedCatalog(
                listOf(
                    RssFeedCatalogEntry("reuters", "Reuters", "top_news", "top news", "http://reuters/top"),
                    RssFeedCatalogEntry("seeking_alpha", "Seeking Alpha", "all_news", "all news", "http://sa/all"),
                    RssFeedCatalogEntry("yahoo_finance", "Yahoo Finance", "ticker", "ticker", "http://yahoo/{symbol}", true)
                )
            ),
            rssFeedClientOverride = RecordingRssFeedClient(rssDao),
            appSettingsStore = FakeAppSettingsStore(
                AppSettings(
                    useMockDataWhenOffline = true,
                    discoveryMode = DiscoveryMode.CUSTOM,
                    riskTolerance = RiskTolerance.MODERATE
                )
            )
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testDispatcher.scheduler)) {
            viewModel.uiState.collect { state ->
                state.progressMessage?.let { progressMessages += it }
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addCustomTicker("AAPL")
        viewModel.addCustomTicker("TSLA")
        viewModel.updateIntent(TradingIntent.SWING)
        viewModel.runAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()
        collector.cancel()

        val state = viewModel.uiState.value
        val result = state.result
        assertNotNull(result)
        assertEquals(null, state.errorMessage)
        assertEquals(false, state.isLoading)
        assertNotNull(state.lastRunAt)
        assertTrue("Router calls=${routerFixture.calls.map { it.stage }} result=$result", result!!.decisionUpdate != null)
        assertTrue("Router calls=${routerFixture.calls.map { it.stage }} result=$result", result.rssDigest != null)
        assertTrue("Router calls=${routerFixture.calls.map { it.stage }} result=$result", result.fundamentalsNewsSynthesis != null)
        assertEquals("AAPL", result.setups.single().symbol)
        assertEquals(NavigationEvent.Results, state.navigationEvent)
        assertEquals(1, historyRepository.savedHistory.size)
        assertEquals(result, historyRepository.savedHistory.single())

        assertProgressSubsequence(
            progressMessages,
            listOf(
                "Stage 1/11: Discovering candidates",
                "Stage 2/11: Filtering tradeable symbols",
                "Stage 3/11: Checking quote providers",
                "Stage 4/11: AI shortlist synthesis",
                "Stage 5/11: Enriching intraday (2 symbols)",
                "Stage 6/11: Enriching fundamentals/context",
                "Stage 7/11: Enriching end-of-day history",
                "Stage 8/11: Ranking setups",
                "Stage 9/11: AI decision update",
                "Stage 10/11: RSS search",
                "Stage 11/11: AI fundamentals/news synthesis"
            )
        )
    }

    @Test
    fun addCustomTickerUpdatesState() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.addCustomTicker("AAPL")

        val state = viewModel.uiState.value
        assertTrue(state.customTickers.any { it.symbol == "AAPL" })
    }

    @Test
    fun initSchedulesAlertsWithStoredInterval() = runTest(testDispatcher) {
        val workScheduler = RecordingWorkScheduler()
        createViewModel(
            alertStore = FakeAlertSettingsStore(settings = AlertSettings(enabled = true)),
            appSettingsStore = FakeAppSettingsStore(
                AppSettings(
                    alertCheckIntervalMinutes = 3,
                    useMockDataWhenOffline = true
                )
            ),
            workScheduler = workScheduler
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(workScheduler.calls.contains(true to 3))
    }

    @Test
    fun unpausingReschedulesAlerts() = runTest(testDispatcher) {
        val workScheduler = RecordingWorkScheduler()
        val appSettingsStore = FakeAppSettingsStore(
            AppSettings(
                alertCheckIntervalMinutes = 3,
                isAnalysisPaused = true,
                useMockDataWhenOffline = true
            )
        )
        val viewModel = createViewModel(
            alertStore = FakeAlertSettingsStore(settings = AlertSettings(enabled = true)),
            appSettingsStore = appSettingsStore,
            workScheduler = workScheduler
        )
        testDispatcher.scheduler.advanceUntilIdle()
        workScheduler.calls.clear()

        viewModel.togglePause()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(workScheduler.calls.contains(true to 3))
    }

    @Test
    fun refreshKeysDiscoversAvailableModelsForOpenAiAndGemini() = runTest(testDispatcher) {
        val discovery = FakeProviderModelDiscovery(
            mapOf(
                LlmProvider.OPENAI to listOf("gpt-5.4", "gpt-5.4-mini", "gpt-5.5"),
                LlmProvider.GEMINI to listOf("models/gemini-3-pro-preview", "models/gemini-3-flash-preview")
            )
        )

        val viewModel = createViewModel(
            llmKeys = LlmKeys(openAiKey = "openai-key", geminiKey = "gemini-key"),
            providerModelDiscovery = discovery
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            listOf(LlmModel.GPT_5_4, LlmModel.GPT_5_4_MINI, LlmModel.GPT_5_5),
            state.availableProviderModels[LlmProvider.OPENAI]
        )
        assertEquals(
            listOf(LlmModel.GEMINI_3_PRO, LlmModel.GEMINI_3_FLASH),
            state.availableProviderModels[LlmProvider.GEMINI]
        )
    }

    @Test
    fun refreshKeysUpgradesLegacyOpenAiDefaultsToDiscoveredFrontierModel() = runTest(testDispatcher) {
        val settingsStore = FakeAppSettingsStore(
            AppSettings(
                llmProvider = LlmProvider.OPENAI,
                analysisModel = LlmModel.GPT_5_1,
                verdictModel = LlmModel.GPT_5_1,
                reasoningModel = LlmModel.GPT_5_2
            )
        )
        val discovery = FakeProviderModelDiscovery(
            mapOf(LlmProvider.OPENAI to listOf("gpt-5.4", "gpt-5.4-mini", "gpt-5.5"))
        )

        val viewModel = createViewModel(
            llmKeys = LlmKeys(openAiKey = "openai-key"),
            appSettingsStore = settingsStore,
            providerModelDiscovery = discovery
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LlmModel.GPT_5_4, state.appSettings.analysisModel)
        assertEquals(LlmModel.GPT_5_4, state.appSettings.verdictModel)
        assertEquals(LlmModel.GPT_5_4, state.appSettings.reasoningModel)
        assertEquals(LlmModel.GPT_5_4, settingsStore.lastSavedSettings?.analysisModel)
    }

    @Test
    fun refreshKeysUpgradesGeminiDefaultsUsingRecommendationTierNotJustFlashName() = runTest(testDispatcher) {
        val settingsStore = FakeAppSettingsStore(
            AppSettings(
                llmProvider = LlmProvider.GEMINI,
                deepDiveProvider = LlmProvider.GEMINI,
                analysisModel = LlmModel.GEMINI_2_5_FLASH,
                verdictModel = LlmModel.GEMINI_2_5_FLASH,
                reasoningModel = LlmModel.GEMINI_2_5_PRO
            )
        )
        val discovery = FakeProviderModelDiscovery(
            mapOf(LlmProvider.GEMINI to listOf("gemini-3-pro-preview", "gemini-3.5-flash", "gemini-3-flash-preview"))
        )

        val viewModel = createViewModel(
            llmKeys = LlmKeys(geminiKey = "gemini-key"),
            appSettingsStore = settingsStore,
            providerModelDiscovery = discovery
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LlmModel.GEMINI_3_5_FLASH, state.appSettings.analysisModel)
        assertEquals(LlmModel.GEMINI_3_5_FLASH, state.appSettings.verdictModel)
        assertEquals(LlmModel.GEMINI_3_5_FLASH, state.appSettings.reasoningModel)
    }

    @Test
    fun refreshKeysCapturesAnthropicModelsWhenConfigured() = runTest(testDispatcher) {
        val discovery = FakeProviderModelDiscovery(
            mapOf(
                LlmProvider.ANTHROPIC to listOf("claude-sonnet-4-5", "claude-opus-4-6", "claude-haiku-4-5")
            )
        )

        val viewModel = createViewModel(
            llmKeys = LlmKeys(anthropicKey = "anthropic-key"),
            providerModelDiscovery = discovery
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            listOf(LlmModel.CLAUDE_SONNET_4_5, LlmModel.CLAUDE_OPUS_4_6, LlmModel.CLAUDE_HAIKU_4_5),
            state.availableProviderModels[LlmProvider.ANTHROPIC]
        )
    }

    @Test
    fun refreshKeysDoesNotOverwriteExplicitAnthropicSelectionWhenStillAvailable() = runTest(testDispatcher) {
        val settingsStore = FakeAppSettingsStore(
            AppSettings(
                llmProvider = LlmProvider.ANTHROPIC,
                deepDiveProvider = LlmProvider.ANTHROPIC,
                analysisModel = LlmModel.CLAUDE_OPUS_4_6,
                verdictModel = LlmModel.CLAUDE_OPUS_4_6,
                reasoningModel = LlmModel.CLAUDE_OPUS_4_6
            )
        )
        val discovery = FakeProviderModelDiscovery(
            mapOf(LlmProvider.ANTHROPIC to listOf("claude-opus-4-6", "claude-sonnet-4-5", "claude-haiku-4-5"))
        )

        val viewModel = createViewModel(
            llmKeys = LlmKeys(anthropicKey = "anthropic-key"),
            appSettingsStore = settingsStore,
            providerModelDiscovery = discovery
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LlmModel.CLAUDE_OPUS_4_6, state.appSettings.analysisModel)
        assertEquals(LlmModel.CLAUDE_OPUS_4_6, state.appSettings.verdictModel)
        assertEquals(LlmModel.CLAUDE_OPUS_4_6, state.appSettings.reasoningModel)
    }

    private fun createViewModel(
        hasKeys: Boolean = false,
        llmKeys: LlmKeys = LlmKeys(),
        mockWhenOffline: Boolean = true,
        providerFactory: MarketDataProviderFactory = FakeProviderFactory(),
        alertStore: AlertSettingsStorage = FakeAlertSettingsStore(),
        workScheduler: WorkScheduler = FakeWorkScheduler(),
        dbRepository: DatabaseRepository = FakeDatabaseRepository(),
        rssDao: com.polaralias.signalsynthesis.data.rss.RssDao = FakeRssDao(),
        stageModelRouterOverride: StageModelRouter? = null,
        rssCatalogOverride: RssFeedCatalog? = null,
        rssFeedClientOverride: RssFeedClient? = null,
        appSettingsStore: AppSettingsStorage = FakeAppSettingsStore(AppSettings(useMockDataWhenOffline = mockWhenOffline)),
        providerModelDiscovery: ProviderModelDiscovery = FakeProviderModelDiscovery()
    ): AnalysisViewModel {
        val keyStore = FakeApiKeyStore(hasKeys, llmKeys)
        return AnalysisViewModel(
            providerFactory = providerFactory,
            keyStore = keyStore,
            alertStore = alertStore,
            workScheduler = workScheduler,
            dbRepository = dbRepository,
            appSettingsStore = appSettingsStore,
            aiSummaryRepository = AiSummaryRepository(FakeAiSummaryDao()),
            rssDao = rssDao,
            application = FakeApplication(),
            stageModelRouterOverride = stageModelRouterOverride,
            rssCatalogOverride = rssCatalogOverride,
            rssFeedClientOverride = rssFeedClientOverride,
            providerModelDiscovery = providerModelDiscovery,
            clock = clock,
            ioDispatcher = testDispatcher
        )
    }

    private class FakeProviderFactory : MarketDataProviderFactory {
        override fun build(keys: ApiKeys): ProviderBundle {
            return ProviderBundle(
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
            )
        }
    }

    private class StaticProviderFactory(
        private val bundle: ProviderBundle
    ) : MarketDataProviderFactory {
        override fun build(keys: ApiKeys): ProviderBundle = bundle
    }

    private class FakeApiKeyStore(
        private val initialHasKeys: Boolean,
        private val llmKeys: LlmKeys = LlmKeys()
    ) : ApiKeyStorage {
        override suspend fun loadApiKeys(): ApiKeys {
            return if (initialHasKeys) ApiKeys(alpacaKey = "test", alpacaSecret = "test") else ApiKeys()
        }
        override suspend fun loadLlmKeys(): LlmKeys = llmKeys
        override suspend fun saveKeys(apiKeys: ApiKeys, llmKeys: LlmKeys) {}
        override suspend fun clear() {}
    }

    private class FakeAlertSettingsStore(
        private var settings: AlertSettings = AlertSettings()
    ) : AlertSettingsStorage {
        override suspend fun loadSettings(): AlertSettings = settings
        override suspend fun saveSettings(settings: AlertSettings) {}
        override suspend fun loadSymbols(): List<String> = emptyList()
        override suspend fun saveSymbols(symbols: List<String>) {}
        override suspend fun loadTargets(): List<com.polaralias.signalsynthesis.data.alerts.AlertTarget> = emptyList()
        override suspend fun saveTargets(targets: List<com.polaralias.signalsynthesis.data.alerts.AlertTarget>) {}
        override suspend fun getLastAlertTimestamp(symbol: String, type: com.polaralias.signalsynthesis.data.alerts.AlertType): Long = 0L
        override suspend fun setLastAlertTimestamp(symbol: String, type: com.polaralias.signalsynthesis.data.alerts.AlertType, timestamp: Long) {}
    }

    private class FakeWorkScheduler : WorkScheduler {
        override fun scheduleAlerts(enabled: Boolean, intervalMinutes: Int) {}
    }

    private class RecordingWorkScheduler : WorkScheduler {
        val calls = mutableListOf<Pair<Boolean, Int>>()

        override fun scheduleAlerts(enabled: Boolean, intervalMinutes: Int) {
            calls += enabled to intervalMinutes
        }
    }

    private class FakeDatabaseRepository : DatabaseRepository {
        override suspend fun addToWatchlist(symbol: String, intent: com.polaralias.signalsynthesis.domain.model.TradingIntent?) {}
        override suspend fun removeFromWatchlist(symbol: String) {}
        override fun getWatchlist(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun saveHistory(result: AnalysisResult) {}
        override fun getHistory(): Flow<List<AnalysisResult>> = flowOf(emptyList())
        override suspend fun clearHistory() {}
    }

    private class RecordingDatabaseRepository : DatabaseRepository {
        val savedHistory = mutableListOf<AnalysisResult>()

        override suspend fun addToWatchlist(symbol: String, intent: TradingIntent?) {}
        override suspend fun removeFromWatchlist(symbol: String) {}
        override fun getWatchlist(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun saveHistory(result: AnalysisResult) {
            savedHistory += result
        }
        override fun getHistory(): Flow<List<AnalysisResult>> = flowOf(emptyList())
        override suspend fun clearHistory() {}
    }

    private class FakeAppSettingsStore(
        private var settings: AppSettings
    ) : AppSettingsStorage {
        var lastSavedSettings: AppSettings? = null

        constructor(mockWhenOffline: Boolean) : this(AppSettings(useMockDataWhenOffline = mockWhenOffline))

        override suspend fun loadSettings(): AppSettings = settings

        override suspend fun saveSettings(settings: AppSettings) {
            this.settings = settings
            this.lastSavedSettings = settings
        }
    }

    private class FakeProviderModelDiscovery(
        private val discoveredModelIds: Map<LlmProvider, List<String>> = emptyMap()
    ) : ProviderModelDiscovery {
        override suspend fun discoverModelIds(provider: LlmProvider, apiKey: String): List<String> {
            return discoveredModelIds[provider].orEmpty()
        }
    }

    private class FakeAiSummaryDao : com.polaralias.signalsynthesis.data.db.dao.AiSummaryDao {
        override suspend fun getByKey(symbol: String, model: String, promptHash: String): com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity? = null
        override fun getAll(): Flow<List<com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity>> = flowOf(emptyList())
        override suspend fun insert(summary: com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity) {}
        override suspend fun delete(summary: com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity) {}
        override suspend fun clear() {}
    }

    private class FakeApplication : android.app.Application()

    private class RecordingRssFeedClient(
        rssDao: com.polaralias.signalsynthesis.data.rss.RssDao
    ) : RssFeedClient(rssDao = rssDao) {
        override suspend fun fetchFeed(url: String) = Unit
    }

    private class FakeRssDao(
        private val items: List<RssItemEntity> = emptyList()
    ) : com.polaralias.signalsynthesis.data.rss.RssDao {
        override suspend fun getFeedState(url: String): RssFeedStateEntity? = null
        override suspend fun insertFeedState(state: RssFeedStateEntity) {}
        override suspend fun getAllRecentItems(since: Long): List<RssItemEntity> = items.filter { it.publishedAt >= since }
        override suspend fun getRecentItemsForFeeds(feedUrls: List<String>, since: Long): List<RssItemEntity> {
            return items.filter { it.feedUrl in feedUrls && it.publishedAt >= since }
        }
        override suspend fun getItemsForFeed(url: String, limit: Int): List<RssItemEntity> = items.filter { it.feedUrl == url }.take(limit)
        override suspend fun insertItems(items: List<RssItemEntity>) {}
        override suspend fun deleteOldItems(threshold: Long) {}
    }

    private class StaticQuoteProvider(
        private val quotes: Map<String, Quote>
    ) : QuoteProvider {
        override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
            return quotes.filterKeys { symbols.contains(it) }
        }
    }

    private class StaticIntradayProvider(
        private val barsBySymbol: Map<String, List<IntradayBar>>
    ) : IntradayProvider {
        override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
            return barsBySymbol[symbol] ?: emptyList()
        }
    }

    private class StaticDailyProvider(
        private val barsBySymbol: Map<String, List<DailyBar>>
    ) : DailyProvider {
        override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
            return barsBySymbol[symbol] ?: emptyList()
        }
    }

    private class StaticProfileProvider(
        private val profiles: Map<String, CompanyProfile>
    ) : ProfileProvider {
        override suspend fun getProfile(symbol: String): CompanyProfile? = profiles[symbol]
    }

    private class StaticMetricsProvider(
        private val metrics: Map<String, FinancialMetrics>
    ) : MetricsProvider {
        override suspend fun getMetrics(symbol: String): FinancialMetrics? = metrics[symbol]
    }

    private class StaticSentimentProvider(
        private val sentiments: Map<String, SentimentData>
    ) : SentimentProvider {
        override suspend fun getSentiment(symbol: String): SentimentData? = sentiments[symbol]
    }

    private fun createIntradayBars(basePrice: Double): List<IntradayBar> {
        val start = Instant.parse("2026-01-01T10:00:00Z")
        return (0 until 20).map { index ->
            IntradayBar(
                time = start.plusSeconds(index * 60L),
                open = basePrice + index,
                high = basePrice + index + 2,
                low = basePrice + index - 2,
                close = basePrice + index + 1,
                volume = 1_000L + index
            )
        }
    }

    private fun createDailyBars(basePrice: Double): List<DailyBar> {
        val start = Instant.parse("2025-01-01T10:00:00Z")
        return (0 until 200).map { index ->
            DailyBar(
                date = LocalDate.ofInstant(start.plusSeconds(index * 86_400L), ZoneOffset.UTC),
                open = basePrice + index,
                high = basePrice + index + 2,
                low = basePrice + index - 2,
                close = basePrice + index + 1,
                volume = 1_000_000L + index
            )
        }
    }

    private fun jsonResponse(json: String): LlmStageResponse {
        return LlmStageResponse(rawText = json, parsedJson = json)
    }

    private fun assertProgressSubsequence(actual: List<String>, expected: List<String>) {
        var position = 0
        actual.forEach { message ->
            if (position < expected.size && message == expected[position]) {
                position += 1
            }
        }
        assertTrue("Expected staged progress subsequence $expected but saw $actual", position == expected.size)
    }
}
