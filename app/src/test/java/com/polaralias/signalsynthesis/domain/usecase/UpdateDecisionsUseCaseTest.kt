package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.LlmStageResponse
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.StageModelConfig
import com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.domain.model.DecisionUpdate
import com.polaralias.signalsynthesis.domain.model.TradeSetup
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
class UpdateDecisionsUseCaseTest {

    @Test
    fun executeParsesAndNormalizesDecisionUpdate() = runTest {
        Logger.clear()
        val json =
            """
            {
              "keep": [
                {
                  "symbol": " aapl ",
                  "confidence": 0.87,
                  "setup_bias": "long",
                  "must_review": ["macro risk"],
                  "rss_needed": true,
                  "expanded_rss_needed": true,
                  "expanded_rss_reason": "Need broader context"
                }
              ],
              "drop": [
                {
                  "symbol": " tsla ",
                  "reasons": ["weak confirmation"]
                }
              ]
            }
            """.trimIndent()
        val stageRouter = stageRouterFixture(
            mapOf(AnalysisStage.DECISION_UPDATE to LlmStageResponse(rawText = json, parsedJson = json))
        )
        val parsed = DecisionUpdate.fromJson(json)
        assertTrue(Logger.logs.value.joinToString(" | ") { "${it.level}:${it.tag}:${it.message}" }, parsed.keep.isNotEmpty())
        assertEquals("AAPL", parsed.keep.single().symbol)

        val useCase = UpdateDecisionsUseCase(stageRouter.router)
        val result = useCase.execute(
            setups = listOf(sampleSetup("AAPL"), sampleSetup("TSLA")),
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE
        )

        assertTrue(Logger.logs.value.joinToString(" | ") { "${it.level}:${it.tag}:${it.message}" }, result.keep.isNotEmpty())
        assertEquals("AAPL", result.keep.single().symbol)
        assertEquals(listOf("macro risk"), result.keep.single().mustReview)
        assertTrue(result.keep.single().rssNeeded)
        assertTrue(result.keep.single().expandedRssNeeded)
        assertEquals("Need broader context", result.keep.single().expandedRssReason)
        assertEquals("TSLA", result.drop.single().symbol)
        assertEquals(listOf("weak confirmation"), result.drop.single().reasons)
    }

    @Test
    fun executeReturnsEmptyUpdateOnMalformedResponse() = runTest {
        val stageRouter = stageRouterFixture(
            mapOf(AnalysisStage.DECISION_UPDATE to LlmStageResponse(rawText = "broken"))
        )

        val useCase = UpdateDecisionsUseCase(stageRouter.router)
        val result = useCase.execute(
            setups = listOf(sampleSetup("AAPL")),
            intent = TradingIntent.DAY_TRADE,
            risk = RiskTolerance.MODERATE
        )

        assertTrue(result.keep.isEmpty())
        assertTrue(result.drop.isEmpty())
    }

    @Test
    fun executeUsesAnthropicRoutingWhenConfigured() = runTest {
        val json =
            """
            {
              "keep": [
                {
                  "symbol": "AAPL",
                  "confidence": 0.87
                }
              ]
            }
            """.trimIndent()
        val stageRouter = stageRouterFixture(
            responses = mapOf(AnalysisStage.DECISION_UPDATE to LlmStageResponse(rawText = json, parsedJson = json)),
            routingConfig = UserModelRoutingConfig(
                byStage = mapOf(
                    AnalysisStage.DECISION_UPDATE to StageModelConfig(
                        provider = LlmProvider.ANTHROPIC,
                        model = "claude-sonnet-4-5"
                    )
                )
            ),
            apiKeys = mapOf(LlmProvider.ANTHROPIC to "test-anthropic-key")
        )

        val useCase = UpdateDecisionsUseCase(stageRouter.router)
        val result = useCase.execute(
            setups = listOf(sampleSetup("AAPL")),
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE
        )

        assertEquals("AAPL", result.keep.single().symbol)
        assertEquals(LlmProvider.ANTHROPIC, stageRouter.calls.single().provider)
        assertEquals("claude-sonnet-4-5", stageRouter.calls.single().model)
    }

    private fun sampleSetup(symbol: String): TradeSetup {
        return TradeSetup(
            symbol = symbol,
            setupType = "Speculative",
            triggerPrice = 100.0,
            stopLoss = 98.0,
            targetPrice = 105.0,
            confidence = 0.5,
            reasons = listOf("sample"),
            validUntil = Instant.parse("2026-01-02T10:00:00Z"),
            intent = TradingIntent.SWING
        )
    }
}
