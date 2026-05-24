package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.LlmStageResponse
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.StageModelConfig
import com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.domain.model.FundamentalsNewsSynthesis
import com.polaralias.signalsynthesis.domain.model.RssDigest
import com.polaralias.signalsynthesis.domain.model.RssHeadline
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
class SynthesizeFundamentalsAndNewsUseCaseTest {

    @Test
    fun executeParsesResponseAndIncludesDigestContext() = runTest {
        Logger.clear()
        val json =
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
        val stageRouter = stageRouterFixture(
            mapOf(
                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS to
                    LlmStageResponse(rawText = json, parsedJson = json)
            )
        )
        val parsed = FundamentalsNewsSynthesis.fromJson(json)
        assertTrue(Logger.logs.value.joinToString(" | ") { "${it.level}:${it.tag}:${it.message}" }, parsed.rankedReviewList.isNotEmpty())
        assertEquals("AAPL", parsed.rankedReviewList.single().symbol)

        val useCase = SynthesizeFundamentalsAndNewsUseCase(stageRouter.router)
        val result = useCase.execute(
            setups = listOf(sampleSetup("AAPL")),
            rssDigest = RssDigest(
                mapOf(
                    "AAPL" to listOf(
                        RssHeadline(
                            title = "AAPL supplier update",
                            link = "http://example.com/aapl",
                            publishedAt = "2026-01-01T10:00:00Z",
                            snippet = "AAPL gains"
                        )
                    )
                )
            ),
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE
        )

        assertTrue(Logger.logs.value.joinToString(" | ") { "${it.level}:${it.tag}:${it.message}" }, result.rankedReviewList.isNotEmpty())
        assertEquals("AAPL", result.rankedReviewList.single().symbol)
        assertEquals(listOf("Check supplier headlines"), result.rankedReviewList.single().whatToReview)
        assertEquals(1, result.portfolioGuidance.positionCount)
        assertTrue(stageRouter.calls.single().request.userPrompt.contains("AAPL supplier update"))
    }

    @Test
    fun executeReturnsEmptySynthesisOnMalformedResponseWithoutDigest() = runTest {
        val stageRouter = stageRouterFixture(
            mapOf(
                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS to
                    LlmStageResponse(rawText = "broken")
            )
        )

        val useCase = SynthesizeFundamentalsAndNewsUseCase(stageRouter.router)
        val result = useCase.execute(
            setups = listOf(sampleSetup("AAPL")),
            rssDigest = null,
            intent = TradingIntent.DAY_TRADE,
            risk = RiskTolerance.MODERATE
        )

        assertTrue(result.rankedReviewList.isEmpty())
        assertEquals(0, result.portfolioGuidance.positionCount)
        assertTrue(stageRouter.calls.single().request.userPrompt.contains("No recent headlines."))
    }

    @Test
    fun executeUsesAnthropicRoutingWhenConfigured() = runTest {
        val json =
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
        val stageRouter = stageRouterFixture(
            responses = mapOf(
                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS to
                    LlmStageResponse(rawText = json, parsedJson = json)
            ),
            routingConfig = UserModelRoutingConfig(
                byStage = mapOf(
                    AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS to StageModelConfig(
                        provider = LlmProvider.ANTHROPIC,
                        model = "claude-sonnet-4-5"
                    )
                )
            ),
            apiKeys = mapOf(LlmProvider.ANTHROPIC to "test-anthropic-key")
        )

        val useCase = SynthesizeFundamentalsAndNewsUseCase(stageRouter.router)
        val result = useCase.execute(
            setups = listOf(sampleSetup("AAPL")),
            rssDigest = null,
            intent = TradingIntent.SWING,
            risk = RiskTolerance.MODERATE
        )

        assertEquals("AAPL", result.rankedReviewList.single().symbol)
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
