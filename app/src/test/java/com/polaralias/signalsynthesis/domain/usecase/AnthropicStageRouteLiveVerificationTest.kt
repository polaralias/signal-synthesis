package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.ai.AnthropicService
import com.polaralias.signalsynthesis.data.ai.AnthropicStageRunner
import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.StageModelConfig
import com.polaralias.signalsynthesis.domain.ai.StageModelRouter
import com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.RssDigest
import com.polaralias.signalsynthesis.domain.model.RssHeadline
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class AnthropicStageRouteLiveVerificationTest {

    @Test
    fun executeRealAnthropicStagesAndWriteVerificationReport() = runBlocking {
        val apiKey = System.getenv("SIGNAL_SYNTHESIS_ANTHROPIC_KEY").orEmpty()
        assumeTrue("SIGNAL_SYNTHESIS_ANTHROPIC_KEY is required", apiKey.isNotBlank())

        val stageEvents = mutableListOf<String>()
        val routing = UserModelRoutingConfig(
            byStage = mapOf(
                AnalysisStage.SHORTLIST to StageModelConfig(
                    provider = LlmProvider.ANTHROPIC,
                    model = "claude-sonnet-4-5",
                    timeoutMs = 90_000L
                ),
                AnalysisStage.DECISION_UPDATE to StageModelConfig(
                    provider = LlmProvider.ANTHROPIC,
                    model = "claude-sonnet-4-5",
                    timeoutMs = 90_000L
                ),
                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS to StageModelConfig(
                    provider = LlmProvider.ANTHROPIC,
                    model = "claude-sonnet-4-5",
                    timeoutMs = 90_000L
                )
            )
        )
        val stageRouter = StageModelRouter(
            runnerFactory = { provider, model, key ->
                RecordingStageRunner(
                    delegate = AnthropicStageRunner(AnthropicService.create(), model, key),
                    stageEvents = stageEvents,
                    provider = provider,
                    model = model
                )
            },
            routingConfigProvider = { routing },
            apiKeysProvider = { mapOf(LlmProvider.ANTHROPIC to apiKey) }
        )

        val shortlist = ShortlistCandidatesUseCase(stageRouter).execute(
            symbols = listOf("AAPL", "MSFT", "NVDA"),
            quotes = sampleQuotes(),
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE,
            maxShortlist = 3
        )
        assertTrue("Expected shortlist response from Anthropic", shortlist.shortlist.isNotEmpty() || shortlist.globalNotes.isNotEmpty())

        val setups = listOf(
            sampleSetup("AAPL", 190.0, 187.0, 198.0, 0.71),
            sampleSetup("MSFT", 425.0, 418.0, 438.0, 0.67)
        )
        val decisionUpdate = UpdateDecisionsUseCase(stageRouter).execute(
            setups = setups,
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE,
            maxKeep = 2
        )

        val synthesis = SynthesizeFundamentalsAndNewsUseCase(stageRouter).execute(
            setups = setups,
            rssDigest = RssDigest(
                mapOf(
                    "AAPL" to listOf(
                        RssHeadline(
                            title = "AAPL supplier update supports device demand outlook",
                            link = "http://example.com/aapl",
                            publishedAt = "2026-05-24T08:00:00Z",
                            snippet = "Supplier guidance improved."
                        )
                    ),
                    "MSFT" to listOf(
                        RssHeadline(
                            title = "MSFT cloud demand remains firm",
                            link = "http://example.com/msft",
                            publishedAt = "2026-05-24T08:10:00Z",
                            snippet = "Cloud channel checks remain healthy."
                        )
                    )
                )
            ),
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE
        )
        assertTrue("Expected no Anthropic stage errors, got $stageEvents", stageEvents.none { it.contains(":error:") })
        assertTrue("Expected synthesis output from Anthropic", synthesis.rankedReviewList.isNotEmpty())

        val reportPath = writeReport(
            AnthropicStageRouteReport(
                runDate = "2026-05-24",
                model = "claude-sonnet-4-5",
                shortlistSymbols = shortlist.shortlist.map { it.symbol },
                shortlistGlobalNotes = shortlist.globalNotes,
                decisionKeepSymbols = decisionUpdate.keep.map { it.symbol },
                decisionDropSymbols = decisionUpdate.drop.map { it.symbol },
                synthesisReviewSymbols = synthesis.rankedReviewList.map { it.symbol },
                stageEvents = stageEvents
            )
        )

        println("ANTHROPIC_STAGE_ROUTE_REPORT=${reportPath.toAbsolutePath()}")
    }

    private fun sampleQuotes(): Map<String, Quote> {
        return mapOf(
            "AAPL" to Quote("AAPL", 190.0, 12_000_000L, Instant.parse("2026-05-24T08:00:00Z"), changePercent = 1.1),
            "MSFT" to Quote("MSFT", 425.0, 9_000_000L, Instant.parse("2026-05-24T08:00:00Z"), changePercent = 0.6),
            "NVDA" to Quote("NVDA", 121.0, 21_000_000L, Instant.parse("2026-05-24T08:00:00Z"), changePercent = -0.3)
        )
    }

    private fun sampleSetup(
        symbol: String,
        triggerPrice: Double,
        stopLoss: Double,
        targetPrice: Double,
        confidence: Double
    ): TradeSetup {
        return TradeSetup(
            symbol = symbol,
            setupType = "Momentum continuation",
            triggerPrice = triggerPrice,
            stopLoss = stopLoss,
            targetPrice = targetPrice,
            confidence = confidence,
            reasons = listOf("Relative strength", "Trend continuation"),
            validUntil = Instant.parse("2026-05-25T16:00:00Z"),
            intent = TradingIntent.SWING
        )
    }

    private fun writeReport(report: AnthropicStageRouteReport): Path {
        val reportDir = Path.of("build", "reports", "live-verification")
        Files.createDirectories(reportDir)
        val reportPath = reportDir.resolve("anthropic-stage-route-live-report.json")
        Files.write(reportPath, report.toJson().toString(2).toByteArray(Charsets.UTF_8))
        return reportPath
    }

    private data class AnthropicStageRouteReport(
        val runDate: String,
        val model: String,
        val shortlistSymbols: List<String>,
        val shortlistGlobalNotes: List<String>,
        val decisionKeepSymbols: List<String>,
        val decisionDropSymbols: List<String>,
        val synthesisReviewSymbols: List<String>,
        val stageEvents: List<String>
    ) {
        fun toJson(): JSONObject {
            return JSONObject()
                .put("runDate", runDate)
                .put("model", model)
                .put("shortlistSymbols", JSONArray(shortlistSymbols))
                .put("shortlistGlobalNotes", JSONArray(shortlistGlobalNotes))
                .put("decisionKeepSymbols", JSONArray(decisionKeepSymbols))
                .put("decisionDropSymbols", JSONArray(decisionDropSymbols))
                .put("synthesisReviewSymbols", JSONArray(synthesisReviewSymbols))
                .put("stageEvents", JSONArray(stageEvents))
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
