package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.data.rss.RssDao
import com.polaralias.signalsynthesis.data.rss.RssFeedCatalog
import com.polaralias.signalsynthesis.data.rss.RssFeedCatalogEntry
import com.polaralias.signalsynthesis.data.rss.RssFeedClient
import com.polaralias.signalsynthesis.data.rss.RssFeedStateEntity
import com.polaralias.signalsynthesis.data.rss.RssItemEntity
import com.polaralias.signalsynthesis.data.settings.DiscoveryMode
import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.StageModelConfig
import com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig
import com.polaralias.signalsynthesis.domain.ai.LlmStageResponse
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.DailyBar
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.IntradayBar
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.SentimentData
import com.polaralias.signalsynthesis.domain.model.TickerSource
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.domain.provider.DailyProvider
import com.polaralias.signalsynthesis.domain.provider.IntradayProvider
import com.polaralias.signalsynthesis.domain.provider.MetricsProvider
import com.polaralias.signalsynthesis.domain.provider.ProfileProvider
import com.polaralias.signalsynthesis.domain.provider.QuoteProvider
import com.polaralias.signalsynthesis.domain.provider.SentimentProvider
import com.polaralias.signalsynthesis.domain.rss.RssFeedSelection
import com.polaralias.signalsynthesis.testsupport.stageRouterFixture
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RunAnalysisV2UseCaseTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneId.of("UTC"))

    @Test
    fun executeProducesDeterministicArtifactsAndHonorsTargetedEnrichment() = runTest {
        val quotes = mapOf(
            "AAPL" to Quote("AAPL", 150.0, 10_000_000L, Instant.now(fixedClock), changePercent = 1.2),
            "TSLA" to Quote("TSLA", 220.0, 12_000_000L, Instant.now(fixedClock), changePercent = -0.4),
            "MSFT" to Quote("MSFT", 310.0, 11_000_000L, Instant.now(fixedClock), changePercent = 0.3)
        )

        val intradayProvider = RecordingIntradayProvider(
            mapOf("AAPL" to createIntradayBars(100.0))
        )
        val dailyProvider = RecordingDailyProvider(
            mapOf("TSLA" to createDailyBars(200.0))
        )
        val profileProvider = RecordingProfileProvider(
            mapOf("AAPL" to CompanyProfile("Apple Inc.", "Technology", "Consumer Electronics", "Maker of phones"))
        )
        val metricsProvider = RecordingMetricsProvider(
            mapOf("AAPL" to FinancialMetrics(2_000_000_000_000L, 30.0, 6.0, "2026-02-15"))
        )
        val sentimentProvider = RecordingSentimentProvider(
            mapOf("AAPL" to SentimentData(0.6, "Bullish"))
        )

        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = listOf(StaticQuoteProvider(quotes)),
                intradayProviders = listOf(intradayProvider),
                dailyProviders = listOf(dailyProvider),
                profileProviders = listOf(profileProvider),
                metricsProviders = listOf(metricsProvider),
                sentimentProviders = listOf(sentimentProvider)
            )
        )

        val stageRouter = stageRouterFixture(
            mapOf(
                AnalysisStage.SHORTLIST to jsonResponse(
                    """
                    {
                      "shortlist": [
                        {
                          "symbol": " aapl ",
                          "priority": 0.91,
                          "reasons": ["price action"],
                          "requested_enrichment": ["INTRADAY", "FUNDAMENTALS"]
                        },
                        {
                          "symbol": "TSLA",
                          "priority": 0.72,
                          "reasons": ["trend continuation"],
                          "requested_enrichment": ["EOD"]
                        },
                        {
                          "symbol": "MSFT",
                          "priority": 0.10,
                          "avoid": true,
                          "risk_flags": ["overextended"]
                        },
                        {
                          "symbol": "NOTREAL",
                          "priority": 0.55,
                          "requested_enrichment": ["INTRADAY"]
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
                          "symbol": "aapl",
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
                          "symbol": "tsla",
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
        val rssClient = RecordingRssFeedClient(rssDao)
        val rssDigestBuilder = BuildRssDigestUseCase(rssClient, rssDao)

        val rssCatalog = RssFeedCatalog(
            listOf(
                RssFeedCatalogEntry("reuters", "Reuters", "top_news", "top news", "http://reuters/top"),
                RssFeedCatalogEntry("seeking_alpha", "Seeking Alpha", "all_news", "all news", "http://sa/all"),
                RssFeedCatalogEntry("yahoo_finance", "Yahoo Finance", "ticker", "ticker", "http://yahoo/{symbol}", true)
            )
        )
        val rssSelection = RssFeedSelection(
            enabledTopicKeys = setOf("reuters:top_news", "seeking_alpha:all_news"),
            enabledTickerSourceIds = setOf("yahoo_finance"),
            useTickerFeedsForFinalStage = true,
            forceExpandedForAll = false
        )

        val progress = mutableListOf<String>()
        val useCase = RunAnalysisV2UseCase(repository, stageRouter.router, rssDigestBuilder, fixedClock)

        val result = useCase.execute(
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE,
            discoveryMode = DiscoveryMode.CUSTOM,
            customTickers = listOf("AAPL", "TSLA", "MSFT"),
            rssSelection = rssSelection,
            rssCatalog = rssCatalog,
            onProgress = progress::add
        )

        assertEquals(3, result.totalCandidates)
        assertEquals(3, result.tradeableCount)
        assertEquals(1, result.setupCount)
        assertEquals(listOf("Watch mega-cap leadership"), result.globalNotes)

        val setup = result.setups.single()
        assertEquals("AAPL", setup.symbol)
        assertEquals(TickerSource.CUSTOM, setup.source)
        assertEquals("long", setup.setupBias)
        assertEquals(listOf("earnings timing"), setup.mustReview)
        assertTrue(setup.rssNeeded)
        assertTrue(setup.expandedRssNeeded)
        assertEquals("Supply-chain checks", setup.expandedRssReason)
        assertEquals(0.88, setup.decisionConfidence ?: 0.0, 0.0001)
        assertNotNull(setup.intradayStats)
        assertNotNull(setup.profile)
        assertNotNull(setup.metrics)
        assertNotNull(setup.sentiment)
        assertNull(setup.eodStats)

        assertEquals(listOf("AAPL"), intradayProvider.requestedSymbols)
        assertEquals(listOf("TSLA"), dailyProvider.requestedSymbols)
        assertEquals(listOf("AAPL"), profileProvider.requestedSymbols)
        assertEquals(listOf("AAPL"), metricsProvider.requestedSymbols)
        assertEquals(listOf("AAPL"), sentimentProvider.requestedSymbols)

        assertNotNull(result.decisionUpdate)
        assertNotNull(result.rssDigest)
        assertTrue(result.rssDigest!!.itemsBySymbol.containsKey("AAPL"))
        assertEquals("AAPL supplier update", result.rssDigest!!.itemsBySymbol.getValue("AAPL").single().title)
        assertNotNull(result.fundamentalsNewsSynthesis)
        assertEquals("AAPL", result.fundamentalsNewsSynthesis!!.rankedReviewList.single().symbol)
        assertEquals(1, result.fundamentalsNewsSynthesis!!.portfolioGuidance.positionCount)

        assertTrue(rssClient.fetchedUrls.contains("http://reuters/top"))
        assertTrue(rssClient.fetchedUrls.contains("http://sa/all"))
        assertTrue(rssClient.fetchedUrls.contains("http://yahoo/AAPL"))
        assertEquals(
            listOf(
                AnalysisStage.SHORTLIST,
                AnalysisStage.DECISION_UPDATE,
                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS
            ),
            stageRouter.calls.map { it.stage }
        )
        assertTrue(progress.any { it.startsWith("Stage 10/11: RSS search") })
        assertEquals("Stage 11/11: AI fundamentals/news synthesis", progress.last())
    }

    @Test
    fun executeReturnsGlobalNotesWhenShortlistFiltersEverything() = runTest {
        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = listOf(
                    StaticQuoteProvider(
                        mapOf("AAPL" to Quote("AAPL", 150.0, 10_000_000L, Instant.now(fixedClock)))
                    )
                ),
                intradayProviders = emptyList(),
                dailyProviders = emptyList(),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        )

        val stageRouter = stageRouterFixture(
            mapOf(
                AnalysisStage.SHORTLIST to jsonResponse(
                    """
                    {
                      "shortlist": [
                        {"symbol": "AAPL", "avoid": true, "risk_flags": ["event risk"]}
                      ],
                      "global_notes": ["Stand aside for now"]
                    }
                    """.trimIndent()
                )
            )
        )

        val useCase = RunAnalysisV2UseCase(repository, stageRouter.router, clock = fixedClock)
        val result = useCase.execute(
            intent = TradingIntent.DAY_TRADE,
            discoveryMode = DiscoveryMode.CUSTOM,
            customTickers = listOf("AAPL")
        )

        assertEquals(1, result.totalCandidates)
        assertEquals(1, result.tradeableCount)
        assertEquals(0, result.setupCount)
        assertTrue(result.setups.isEmpty())
        assertEquals(listOf("Stand aside for now"), result.globalNotes)
        assertNull(result.decisionUpdate)
        assertEquals(listOf(AnalysisStage.SHORTLIST), stageRouter.calls.map { it.stage })
    }

    @Test
    fun executeCanRunEntireStagedPathThroughAnthropicRouting() = runTest {
        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = listOf(
                    StaticQuoteProvider(
                        mapOf("AAPL" to Quote("AAPL", 150.0, 10_000_000L, Instant.now(fixedClock), changePercent = 1.2))
                    )
                ),
                intradayProviders = emptyList(),
                dailyProviders = emptyList(),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        )
        val anthropicRouting = UserModelRoutingConfig(
            byStage = mapOf(
                AnalysisStage.SHORTLIST to StageModelConfig(provider = LlmProvider.ANTHROPIC, model = "claude-sonnet-4-5"),
                AnalysisStage.DECISION_UPDATE to StageModelConfig(provider = LlmProvider.ANTHROPIC, model = "claude-sonnet-4-5"),
                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS to StageModelConfig(provider = LlmProvider.ANTHROPIC, model = "claude-sonnet-4-5")
            )
        )
        val stageRouter = stageRouterFixture(
            responses = mapOf(
                AnalysisStage.SHORTLIST to jsonResponse(
                    """
                    {
                      "shortlist": [{"symbol": "AAPL", "priority": 0.9}],
                      "global_notes": ["Claude shortlist note"]
                    }
                    """.trimIndent()
                ),
                AnalysisStage.DECISION_UPDATE to jsonResponse(
                    """
                    {
                      "keep": [{"symbol": "AAPL", "confidence": 0.75}]
                    }
                    """.trimIndent()
                ),
                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS to jsonResponse(
                    """
                    {
                      "ranked_review_list": [
                        {
                          "symbol": "AAPL",
                          "what_to_review": ["Check leadership strength"],
                          "risk_summary": ["Single-name risk"],
                          "one_paragraph_brief": "Setup still holds."
                        }
                      ],
                      "portfolio_guidance": {
                        "position_count": 1,
                        "risk_posture": "moderate"
                      }
                    }
                    """.trimIndent()
                )
            ),
            routingConfig = anthropicRouting,
            apiKeys = mapOf(LlmProvider.ANTHROPIC to "test-anthropic-key")
        )

        val useCase = RunAnalysisV2UseCase(repository, stageRouter.router, clock = fixedClock)
        val result = useCase.execute(
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE,
            discoveryMode = DiscoveryMode.CUSTOM,
            customTickers = listOf("AAPL")
        )

        assertEquals(1, result.setupCount)
        assertEquals(listOf("Claude shortlist note"), result.globalNotes)
        assertEquals(
            listOf(LlmProvider.ANTHROPIC, LlmProvider.ANTHROPIC, LlmProvider.ANTHROPIC),
            stageRouter.calls.map { it.provider }
        )
        assertEquals(
            listOf("claude-sonnet-4-5", "claude-sonnet-4-5", "claude-sonnet-4-5"),
            stageRouter.calls.map { it.model }
        )
    }

    private fun jsonResponse(json: String): LlmStageResponse {
        return LlmStageResponse(rawText = json, parsedJson = json)
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

    private class StaticQuoteProvider(
        private val quotes: Map<String, Quote>
    ) : QuoteProvider {
        override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
            return quotes.filterKeys { symbols.contains(it) }
        }
    }

    private class RecordingIntradayProvider(
        private val barsBySymbol: Map<String, List<IntradayBar>>
    ) : IntradayProvider {
        val requestedSymbols = mutableListOf<String>()

        override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
            requestedSymbols += symbol
            return barsBySymbol[symbol] ?: emptyList()
        }
    }

    private class RecordingDailyProvider(
        private val barsBySymbol: Map<String, List<DailyBar>>
    ) : DailyProvider {
        val requestedSymbols = mutableListOf<String>()

        override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
            requestedSymbols += symbol
            return barsBySymbol[symbol] ?: emptyList()
        }
    }

    private class RecordingProfileProvider(
        private val profiles: Map<String, CompanyProfile>
    ) : ProfileProvider {
        val requestedSymbols = mutableListOf<String>()

        override suspend fun getProfile(symbol: String): CompanyProfile? {
            requestedSymbols += symbol
            return profiles[symbol]
        }
    }

    private class RecordingMetricsProvider(
        private val metrics: Map<String, FinancialMetrics>
    ) : MetricsProvider {
        val requestedSymbols = mutableListOf<String>()

        override suspend fun getMetrics(symbol: String): FinancialMetrics? {
            requestedSymbols += symbol
            return metrics[symbol]
        }
    }

    private class RecordingSentimentProvider(
        private val sentiments: Map<String, SentimentData>
    ) : SentimentProvider {
        val requestedSymbols = mutableListOf<String>()

        override suspend fun getSentiment(symbol: String): SentimentData? {
            requestedSymbols += symbol
            return sentiments[symbol]
        }
    }

    private class RecordingRssFeedClient(
        rssDao: RssDao
    ) : RssFeedClient(rssDao = rssDao) {
        val fetchedUrls = mutableListOf<String>()

        override suspend fun fetchFeed(url: String) {
            fetchedUrls += url
        }
    }

    private class FakeRssDao(
        private val items: List<RssItemEntity>
    ) : RssDao {
        override suspend fun getFeedState(url: String): RssFeedStateEntity? = null

        override suspend fun insertFeedState(state: RssFeedStateEntity) = Unit

        override suspend fun getAllRecentItems(since: Long): List<RssItemEntity> {
            return items.filter { it.publishedAt >= since }
        }

        override suspend fun getRecentItemsForFeeds(feedUrls: List<String>, since: Long): List<RssItemEntity> {
            return items.filter { it.feedUrl in feedUrls && it.publishedAt >= since }
        }

        override suspend fun getItemsForFeed(url: String, limit: Int): List<RssItemEntity> {
            return items.filter { it.feedUrl == url }.take(limit)
        }

        override suspend fun insertItems(items: List<RssItemEntity>) = Unit

        override suspend fun deleteOldItems(threshold: Long) = Unit
    }
}
