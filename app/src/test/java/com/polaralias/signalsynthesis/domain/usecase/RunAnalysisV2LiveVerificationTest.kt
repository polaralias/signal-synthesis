package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.ai.OpenAiResponsesService
import com.polaralias.signalsynthesis.data.ai.OpenAiStageRunner
import com.polaralias.signalsynthesis.data.ai.AnthropicService
import com.polaralias.signalsynthesis.data.ai.AnthropicStageRunner
import com.polaralias.signalsynthesis.data.ai.GeminiService
import com.polaralias.signalsynthesis.data.ai.GeminiStageRunner
import com.polaralias.signalsynthesis.data.provider.ApiKeys
import com.polaralias.signalsynthesis.data.provider.ProviderFactory
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.data.rss.RssDao
import com.polaralias.signalsynthesis.data.rss.RssFeedCatalog
import com.polaralias.signalsynthesis.data.rss.RssFeedCatalogEntry
import com.polaralias.signalsynthesis.data.rss.RssFeedClient
import com.polaralias.signalsynthesis.data.rss.RssFeedStateEntity
import com.polaralias.signalsynthesis.data.rss.RssItemEntity
import com.polaralias.signalsynthesis.data.settings.AssetClass
import com.polaralias.signalsynthesis.data.settings.DiscoveryMode
import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.StageModelConfig
import com.polaralias.signalsynthesis.domain.ai.StageModelRouter
import com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.TickerSource
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.domain.rss.RssFeedResolver
import com.polaralias.signalsynthesis.domain.rss.RssFeedSelection
import com.polaralias.signalsynthesis.domain.rss.RssFeedStage
import com.polaralias.signalsynthesis.domain.rss.RssTickerInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class RunAnalysisV2LiveVerificationTest {

    @Test
    fun executeLiveProviderScenarioAndWriteVerificationReport() = runBlocking {
        val env = LiveEnv.fromSystem()
        assumeTrue("Live verification env vars are required", env.isConfigured())

        val stageEvents = mutableListOf<String>()
        val repository = MarketDataRepository(ProviderFactory(includeMock = false).build(env.apiKeys))
        val stageProvider = env.stageProvider
        val stageModel = when (stageProvider) {
            LlmProvider.OPENAI -> "gpt-5.4"
            LlmProvider.ANTHROPIC -> "claude-sonnet-4-5"
            LlmProvider.GEMINI -> "gemini-3-flash-preview"
            else -> error("Unsupported live stage provider $stageProvider")
        }
        val stageTimeoutMs = when (stageProvider) {
            LlmProvider.OPENAI -> 90_000L
            LlmProvider.ANTHROPIC -> 90_000L
            LlmProvider.GEMINI -> 30_000L
            else -> 30_000L
        }
        val liveRouting = UserModelRoutingConfig(
            byStage = mapOf(
                AnalysisStage.SHORTLIST to StageModelConfig(
                    provider = stageProvider,
                    model = stageModel,
                    timeoutMs = stageTimeoutMs
                ),
                AnalysisStage.DECISION_UPDATE to StageModelConfig(
                    provider = stageProvider,
                    model = stageModel,
                    timeoutMs = stageTimeoutMs
                ),
                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS to StageModelConfig(
                    provider = stageProvider,
                    model = stageModel,
                    timeoutMs = stageTimeoutMs
                )
            )
        )
        val stageRouter = StageModelRouter(
            runnerFactory = { provider, model, key ->
                when (provider) {
                    LlmProvider.OPENAI -> RecordingStageRunner(
                        delegate = OpenAiStageRunner(OpenAiResponsesService.create(), model, key),
                        stageEvents = stageEvents,
                        provider = provider,
                        model = model
                    )
                    LlmProvider.ANTHROPIC -> RecordingStageRunner(
                        delegate = AnthropicStageRunner(AnthropicService.create(), model, key),
                        stageEvents = stageEvents,
                        provider = provider,
                        model = model
                    )
                    LlmProvider.GEMINI -> RecordingStageRunner(
                        delegate = GeminiStageRunner(GeminiService.create(), model, key),
                        stageEvents = stageEvents,
                        provider = provider,
                        model = model
                    )
                    else -> error("Live verification currently supports OpenAI, Anthropic, or Gemini routed stages only, got $provider")
                }
            },
            routingConfigProvider = { liveRouting },
            apiKeysProvider = {
                mapOf(
                    LlmProvider.OPENAI to env.openAiKey,
                    LlmProvider.ANTHROPIC to env.anthropicKey,
                    LlmProvider.GEMINI to env.geminiKey
                )
            }
        )
        val rssDao = InMemoryRssDao()
        val rssFeedClient = RssFeedClient(rssDao = rssDao)
        val rssDigestBuilder = BuildRssDigestUseCase(rssFeedClient, rssDao)
        val rssCatalog = liveRssCatalog()
        val rssSelection = RssFeedSelection(
            enabledTopicKeys = setOf("seeking_alpha:all_news"),
            enabledTickerSourceIds = setOf("yahoo_finance"),
            useTickerFeedsForFinalStage = true,
            forceExpandedForAll = false
        )
        val clock = Clock.fixed(Instant.parse("2026-05-23T12:00:00Z"), ZoneOffset.UTC)

        val discoverCandidates = DiscoverCandidatesUseCase(repository)
        val filterTradeable = FilterTradeableUseCase(repository)
        val shortlistCandidates = ShortlistCandidatesUseCase(stageRouter)
        val enrichIntraday = EnrichIntradayUseCase(repository)
        val enrichContext = EnrichContextUseCase(repository)
        val enrichEod = EnrichEodUseCase(repository)
        val rankSetups = RankSetupsUseCase(clock)
        val updateDecisions = UpdateDecisionsUseCase(stageRouter)
        val synthesize = SynthesizeFundamentalsAndNewsUseCase(stageRouter)
        val rssFeedResolver = RssFeedResolver()

        val intent = TradingIntent.SWING
        val risk = RiskTolerance.MODERATE
        val assetClass = AssetClass.STOCKS
        val discoveryMode = DiscoveryMode.CUSTOM
        val customTickers = listOf("AAPL", "MSFT", "NVDA", "TSLA", "AMZN", "META")

        val candidateMap = discoverCandidates.execute(
            intent = intent,
            risk = risk,
            assetClass = assetClass,
            discoveryMode = discoveryMode,
            customTickers = customTickers,
            screenerThresholds = emptyMap()
        )
        val discovered = candidateMap.keys.toList()
        assertTrue("Expected discovered universe to be non-empty", discovered.isNotEmpty())

        val tradeable = filterTradeable.execute(discovered, minPrice = 1.0)
        assertTrue("Expected at least one tradeable symbol from discovered=$discovered", tradeable.isNotEmpty())

        val quotes = repository.getQuotes(tradeable)
        assertTrue("Expected live quotes for at least one tradeable symbol", quotes.isNotEmpty())

        val shortlistPlan = shortlistCandidates.execute(
            symbols = tradeable,
            quotes = quotes,
            intent = intent,
            risk = risk,
            maxShortlist = 4
        )
        assertTrue(
            "Expected shortlist to contain at least one symbol. tradeable=$tradeable notes=${shortlistPlan.globalNotes} stageEvents=$stageEvents",
            shortlistPlan.shortlist.isNotEmpty()
        )

        val tradeableByUpper = tradeable.associateBy { it.uppercase(Locale.US) }
        val shortlistedSymbols = shortlistPlan.shortlist
            .asSequence()
            .filter { !it.avoid }
            .map { it.symbol.trim().uppercase(Locale.US) }
            .mapNotNull { tradeableByUpper[it] }
            .distinct()
            .toList()
        assertTrue("Expected at least one shortlisted symbol after filtering", shortlistedSymbols.isNotEmpty())

        val shortlistBySymbol = shortlistPlan.shortlist.associateBy { it.symbol.trim().uppercase(Locale.US) }
        val symbolsWithoutExplicitRequests = shortlistedSymbols.filter { symbol ->
            shortlistBySymbol[symbol.uppercase(Locale.US)]?.requestedEnrichment.orEmpty().isEmpty()
        }
        val intradayTargets = requestedTargets("INTRADAY", shortlistPlan, tradeableByUpper, shortlistedSymbols) + symbolsWithoutExplicitRequests
        val contextTargets = requestedTargets("FUNDAMENTALS", shortlistPlan, tradeableByUpper, shortlistedSymbols) +
            requestedTargets("SENTIMENT", shortlistPlan, tradeableByUpper, shortlistedSymbols) +
            symbolsWithoutExplicitRequests
        val eodTargets = (requestedTargets("EOD", shortlistPlan, tradeableByUpper, shortlistedSymbols) + symbolsWithoutExplicitRequests).distinct()

        val intradayStats = enrichIntraday.execute(intradayTargets.distinct(), days = 2)
        val contextData = enrichContext.execute(contextTargets.distinct())
        val eodStats = enrichEod.execute(eodTargets, days = 200)

        val rankedSetups = rankSetups.execute(
            symbols = shortlistedSymbols,
            quotes = quotes,
            intradayStats = intradayStats,
            eodStats = eodStats,
            contextData = contextData,
            intent = intent
        )
        assertTrue("Expected ranked setups before decision update", rankedSetups.isNotEmpty())

        val setupsWithSource = rankedSetups.map { setup ->
            setup.copy(source = candidateMap[setup.symbol] ?: TickerSource.PREDEFINED)
        }
        val decisionUpdate = updateDecisions.execute(
            setups = setupsWithSource,
            intent = intent,
            risk = risk,
            maxKeep = 3
        )
        val finalSetups = applyDecisionUpdate(setupsWithSource, decisionUpdate)
        assertTrue("Expected at least one final setup after decision update", finalSetups.isNotEmpty())

        val resolvedFeeds = rssFeedResolver.resolve(
            catalog = rssCatalog,
            selection = rssSelection,
            tickers = finalSetups.map { setup ->
                RssTickerInput(
                    symbol = setup.symbol,
                    source = setup.source,
                    rssNeeded = setup.rssNeeded,
                    expandedRssNeeded = setup.expandedRssNeeded
                )
            },
            stage = RssFeedStage.ANALYSIS
        )
        assertTrue("Expected at least one resolved RSS feed", resolvedFeeds.feedUrls.isNotEmpty())

        val rssDigest = rssDigestBuilder.execute(
            tickers = finalSetups.map { it.symbol },
            feedUrls = resolvedFeeds.feedUrls
        )
        val synthesis = synthesize.execute(
            setups = finalSetups,
            rssDigest = rssDigest,
            intent = intent,
            risk = risk
        )
        assertNotNull("Expected fundamentals/news synthesis", synthesis)

        val result = AnalysisResult(
            intent = intent,
            totalCandidates = discovered.size,
            tradeableCount = tradeable.size,
            setupCount = finalSetups.size,
            setups = finalSetups,
            generatedAt = Instant.now(clock),
            globalNotes = shortlistPlan.globalNotes,
            rssDigest = rssDigest,
            decisionUpdate = decisionUpdate,
            fundamentalsNewsSynthesis = synthesis
        )

        val reportPath = writeReport(
            LiveVerificationReport(
                runDate = "2026-05-23",
                tester = "Codex live verification",
                routing = mapOf(
                    "SHORTLIST" to "${stageProvider.name}/$stageModel",
                    "DECISION_UPDATE" to "${stageProvider.name}/$stageModel",
                    "FUNDAMENTALS_NEWS_SYNTHESIS" to "${stageProvider.name}/$stageModel"
                ),
                inputScenario = mapOf(
                    "intent" to intent.name,
                    "risk" to risk.name,
                    "assetClass" to assetClass.name,
                    "discoveryMode" to discoveryMode.name,
                    "customTickers" to customTickers.joinToString(",")
                ),
                discoveredUniverse = discovered,
                tradeableSymbols = tradeable,
                shortlistSymbols = shortlistPlan.shortlist.map { it.symbol },
                rankedSetupsBeforeDecisionUpdate = rankedSetups.map { it.symbol },
                decisionKeepSymbols = decisionUpdate.keep.map { it.symbol },
                decisionDropSymbols = decisionUpdate.drop.map { it.symbol },
                resolvedFeeds = resolvedFeeds.feedUrls,
                rssDigestTickers = rssDigest.itemsBySymbol.keys.sorted(),
                finalSetupSymbols = result.setups.map { it.symbol },
                globalNotes = result.globalNotes,
                synthesisReviewSymbols = synthesis.rankedReviewList.map { it.symbol },
                synthesisRiskPosture = synthesis.portfolioGuidance.riskPosture
            )
        )

        println("LIVE_VERIFICATION_REPORT=${reportPath.toAbsolutePath()}")
    }

    private fun requestedTargets(
        requestType: String,
        shortlistPlan: com.polaralias.signalsynthesis.domain.model.ShortlistPlan,
        tradeableByUpper: Map<String, String>,
        shortlistedSymbols: List<String>
    ): List<String> {
        return shortlistPlan.shortlist
            .asSequence()
            .filter { it.requestedEnrichment.contains(requestType) }
            .map { it.symbol.trim().uppercase(Locale.US) }
            .mapNotNull { tradeableByUpper[it] }
            .filter { shortlistedSymbols.contains(it) }
            .distinct()
            .toList()
    }

    private fun applyDecisionUpdate(
        setupsWithSource: List<TradeSetup>,
        decisionUpdate: com.polaralias.signalsynthesis.domain.model.DecisionUpdate
    ): List<TradeSetup> {
        val keepSymbols = decisionUpdate.keep
            .map { it.symbol.trim().uppercase(Locale.US) }
            .filter { it.isNotBlank() }
            .toSet()
        val dropSymbols = decisionUpdate.drop
            .map { it.symbol.trim().uppercase(Locale.US) }
            .filter { it.isNotBlank() }
            .toSet()
        val filteredSetups = when {
            keepSymbols.isNotEmpty() -> setupsWithSource.filter { keepSymbols.contains(it.symbol.uppercase(Locale.US)) }
            dropSymbols.isNotEmpty() -> setupsWithSource.filterNot { dropSymbols.contains(it.symbol.uppercase(Locale.US)) }
            else -> setupsWithSource
        }
        val decisionMap = decisionUpdate.keep.associateBy { it.symbol.trim().uppercase(Locale.US) }
        return filteredSetups.map { setup ->
            val decision = decisionMap[setup.symbol.uppercase(Locale.US)] ?: return@map setup
            setup.copy(
                setupBias = decision.setupBias,
                mustReview = decision.mustReview,
                rssNeeded = decision.rssNeeded || decision.expandedRssNeeded,
                expandedRssNeeded = decision.expandedRssNeeded,
                expandedRssReason = decision.expandedRssReason?.takeIf { it.isNotBlank() },
                decisionConfidence = if (decision.confidence > 0.0) decision.confidence else null
            )
        }
    }

    private fun liveRssCatalog(): RssFeedCatalog {
        return RssFeedCatalog(
            listOf(
                RssFeedCatalogEntry(
                    sourceId = "seeking_alpha",
                    sourceLabel = "Seeking Alpha",
                    topicId = "all_news",
                    topicLabel = "all news",
                    url = "https://seekingalpha.com/market_currents.xml"
                ),
                RssFeedCatalogEntry(
                    sourceId = "yahoo_finance",
                    sourceLabel = "Yahoo Finance",
                    topicId = "ticker",
                    topicLabel = "ticker",
                    url = "https://feeds.finance.yahoo.com/rss/2.0/headline?s={}&region=US&lang=en-US",
                    isTickerTemplate = true
                )
            )
        )
    }

    private fun writeReport(report: LiveVerificationReport): Path {
        val reportDir = Path.of("build", "reports", "live-verification")
        Files.createDirectories(reportDir)
        val reportPath = reportDir.resolve("v2-live-report.json")
        Files.write(reportPath, report.toJson().toString(2).toByteArray(Charsets.UTF_8))
        return reportPath
    }

    private data class LiveVerificationReport(
        val runDate: String,
        val tester: String,
        val routing: Map<String, String>,
        val inputScenario: Map<String, String>,
        val discoveredUniverse: List<String>,
        val tradeableSymbols: List<String>,
        val shortlistSymbols: List<String>,
        val rankedSetupsBeforeDecisionUpdate: List<String>,
        val decisionKeepSymbols: List<String>,
        val decisionDropSymbols: List<String>,
        val resolvedFeeds: List<String>,
        val rssDigestTickers: List<String>,
        val finalSetupSymbols: List<String>,
        val globalNotes: List<String>,
        val synthesisReviewSymbols: List<String>,
        val synthesisRiskPosture: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject()
                .put("runDate", runDate)
                .put("tester", tester)
                .put("routing", JSONObject(routing))
                .put("inputScenario", JSONObject(inputScenario))
                .put("discoveredUniverse", JSONArray(discoveredUniverse))
                .put("tradeableSymbols", JSONArray(tradeableSymbols))
                .put("shortlistSymbols", JSONArray(shortlistSymbols))
                .put("rankedSetupsBeforeDecisionUpdate", JSONArray(rankedSetupsBeforeDecisionUpdate))
                .put("decisionKeepSymbols", JSONArray(decisionKeepSymbols))
                .put("decisionDropSymbols", JSONArray(decisionDropSymbols))
                .put("resolvedFeeds", JSONArray(resolvedFeeds))
                .put("rssDigestTickers", JSONArray(rssDigestTickers))
                .put("finalSetupSymbols", JSONArray(finalSetupSymbols))
                .put("globalNotes", JSONArray(globalNotes))
                .put("synthesisReviewSymbols", JSONArray(synthesisReviewSymbols))
                .put("synthesisRiskPosture", synthesisRiskPosture)
        }
    }

    private data class LiveEnv(
        val apiKeys: ApiKeys,
        val openAiKey: String,
        val anthropicKey: String,
        val geminiKey: String,
        val stageProvider: LlmProvider
    ) {
        fun isConfigured(): Boolean {
            val providerKeyPresent = when (stageProvider) {
                LlmProvider.OPENAI -> openAiKey.isNotBlank()
                LlmProvider.ANTHROPIC -> anthropicKey.isNotBlank()
                LlmProvider.GEMINI -> geminiKey.isNotBlank()
                else -> false
            }
            return providerKeyPresent && apiKeys.hasAny()
        }

        companion object {
            fun fromSystem(): LiveEnv {
                val env = System.getenv()
                val provider = env["SIGNAL_SYNTHESIS_LIVE_LLM_PROVIDER"]
                    ?.trim()
                    ?.uppercase(Locale.US)
                    ?.let { name -> runCatching { LlmProvider.valueOf(name) }.getOrNull() }
                    ?: LlmProvider.GEMINI
                return LiveEnv(
                    apiKeys = ApiKeys(
                        alpacaKey = env["SIGNAL_SYNTHESIS_ALPACA_KEY"],
                        alpacaSecret = env["SIGNAL_SYNTHESIS_ALPACA_SECRET"],
                        massive = env["SIGNAL_SYNTHESIS_POLYGON_KEY"],
                        financialModelingPrep = env["SIGNAL_SYNTHESIS_FMP_KEY"],
                        finnhub = env["SIGNAL_SYNTHESIS_FINNHUB_KEY"],
                        twelveData = env["SIGNAL_SYNTHESIS_TWELVEDATA_KEY"]
                    ),
                    openAiKey = env["SIGNAL_SYNTHESIS_OPENAI_KEY"].orEmpty(),
                    anthropicKey = env["SIGNAL_SYNTHESIS_ANTHROPIC_KEY"].orEmpty(),
                    geminiKey = env["SIGNAL_SYNTHESIS_GEMINI_KEY"].orEmpty(),
                    stageProvider = provider
                )
            }
        }
    }

    private class InMemoryRssDao : RssDao {
        private val feedStates = mutableMapOf<String, RssFeedStateEntity>()
        private val items = mutableListOf<RssItemEntity>()

        override suspend fun getFeedState(url: String): RssFeedStateEntity? = feedStates[url]

        override suspend fun insertFeedState(state: RssFeedStateEntity) {
            feedStates[state.feedUrl] = state
        }

        override suspend fun getAllRecentItems(since: Long): List<RssItemEntity> {
            return items.filter { it.publishedAt >= since }
        }

        override suspend fun getRecentItemsForFeeds(feedUrls: List<String>, since: Long): List<RssItemEntity> {
            return items.filter { it.feedUrl in feedUrls && it.publishedAt >= since }
        }

        override suspend fun getItemsForFeed(url: String, limit: Int): List<RssItemEntity> {
            return items.filter { it.feedUrl == url }.take(limit)
        }

        override suspend fun insertItems(items: List<RssItemEntity>) {
            val existing = this.items.map { it.guidHash }.toMutableSet()
            items.forEach { item ->
                if (existing.add(item.guidHash)) {
                    this.items += item
                }
            }
        }

        override suspend fun deleteOldItems(threshold: Long) {
            items.removeAll { it.publishedAt < threshold }
        }
    }

    private class RecordingStageRunner(
        private val delegate: com.polaralias.signalsynthesis.domain.ai.StageLlmRunner,
        private val stageEvents: MutableList<String>,
        private val provider: LlmProvider,
        private val model: String
    ) : com.polaralias.signalsynthesis.domain.ai.StageLlmRunner {
        override suspend fun run(request: com.polaralias.signalsynthesis.domain.ai.LlmStageRequest): com.polaralias.signalsynthesis.domain.ai.LlmStageResponse {
            return try {
                val response = delegate.run(request)
                stageEvents += "${request.stage.name}:${provider.name}/$model:success:${response.rawText.take(200).replace('\n', ' ')}"
                response
            } catch (e: Exception) {
                stageEvents += "${request.stage.name}:${provider.name}/$model:error:${e::class.simpleName}:${e.message}"
                throw e
            }
        }
    }
}
