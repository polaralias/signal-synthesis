package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.LlmStageResponse
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.StageModelConfig
import com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.ShortlistPlan
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.testsupport.stageRouterFixture
import com.polaralias.signalsynthesis.util.Logger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ShortlistCandidatesUseCaseTest {

    @Test
    fun executeParsesValidJsonAndClampsToMaxShortlist() = runTest {
        Logger.clear()
        val json =
            """
            {
              "shortlist": [
                {"symbol": "AAPL", "priority": 0.9, "requested_enrichment": ["INTRADAY"]},
                {"symbol": "TSLA", "priority": 0.8, "requested_enrichment": ["EOD"]},
                {"symbol": "MSFT", "priority": 0.7, "requested_enrichment": ["FUNDAMENTALS"]}
              ],
              "global_notes": ["Keep the list tight"]
            }
            """.trimIndent()
        val stageRouter = stageRouterFixture(
            mapOf(AnalysisStage.SHORTLIST to LlmStageResponse(rawText = json, parsedJson = json))
        )
        val parsed = ShortlistPlan.fromJson(json)
        assertEquals(
            Logger.logs.value.joinToString(" | ") { "${it.level}:${it.tag}:${it.message}" },
            listOf("AAPL", "TSLA", "MSFT"),
            parsed.shortlist.map { it.symbol }
        )

        val useCase = ShortlistCandidatesUseCase(stageRouter.router)
        val result = useCase.execute(
            symbols = listOf("AAPL", "TSLA", "MSFT"),
            quotes = sampleQuotes(),
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE,
            maxShortlist = 2
        )

        assertEquals(
            Logger.logs.value.joinToString(" | ") { "${it.level}:${it.tag}:${it.message}" },
            listOf("AAPL", "TSLA"),
            result.shortlist.map { it.symbol }
        )
        assertEquals(listOf("Keep the list tight"), result.globalNotes)
        assertEquals(2, result.shortlist.size)
    }

    @Test
    fun executeReturnsEmptyPlanOnMalformedResponse() = runTest {
        val stageRouter = stageRouterFixture(
            mapOf(AnalysisStage.SHORTLIST to LlmStageResponse(rawText = "not json"))
        )

        val useCase = ShortlistCandidatesUseCase(stageRouter.router)
        val result = useCase.execute(
            symbols = listOf("AAPL"),
            quotes = sampleQuotes(),
            intent = TradingIntent.DAY_TRADE,
            risk = RiskTolerance.MODERATE
        )

        assertTrue(result.shortlist.isEmpty())
        assertTrue(result.globalNotes.isEmpty())
    }

    @Test
    fun executeUsesAnthropicRoutingWhenConfigured() = runTest {
        val json =
            """
            {
              "shortlist": [
                {"symbol": "AAPL", "priority": 0.9}
              ]
            }
            """.trimIndent()
        val stageRouter = stageRouterFixture(
            responses = mapOf(AnalysisStage.SHORTLIST to LlmStageResponse(rawText = json, parsedJson = json)),
            routingConfig = UserModelRoutingConfig(
                byStage = mapOf(
                    AnalysisStage.SHORTLIST to StageModelConfig(
                        provider = LlmProvider.ANTHROPIC,
                        model = "claude-sonnet-4-5"
                    )
                )
            ),
            apiKeys = mapOf(LlmProvider.ANTHROPIC to "test-anthropic-key")
        )

        val useCase = ShortlistCandidatesUseCase(stageRouter.router)
        val result = useCase.execute(
            symbols = listOf("AAPL"),
            quotes = sampleQuotes(),
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE
        )

        assertEquals(listOf("AAPL"), result.shortlist.map { it.symbol })
        assertEquals(LlmProvider.ANTHROPIC, stageRouter.calls.single().provider)
        assertEquals("claude-sonnet-4-5", stageRouter.calls.single().model)
    }

    private fun sampleQuotes(): Map<String, Quote> {
        return mapOf(
            "AAPL" to Quote("AAPL", 150.0, 1_000_000L, Instant.parse("2026-01-01T10:00:00Z")),
            "TSLA" to Quote("TSLA", 200.0, 1_000_000L, Instant.parse("2026-01-01T10:00:00Z")),
            "MSFT" to Quote("MSFT", 300.0, 1_000_000L, Instant.parse("2026-01-01T10:00:00Z"))
        )
    }
}
