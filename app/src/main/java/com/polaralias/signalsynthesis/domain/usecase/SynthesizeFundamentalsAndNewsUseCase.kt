package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.AiPrompts
import com.polaralias.signalsynthesis.domain.ai.LlmStageRequest
import com.polaralias.signalsynthesis.domain.ai.StageModelRouter
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.domain.model.FundamentalsNewsSynthesis
import com.polaralias.signalsynthesis.domain.model.RssDigest
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.util.JsonExtraction
import com.polaralias.signalsynthesis.util.Logger
import java.util.Locale

class SynthesizeFundamentalsAndNewsUseCase(
    private val stageModelRouter: StageModelRouter
) {
    suspend fun execute(
        setups: List<TradeSetup>,
        rssDigest: RssDigest?,
        intent: TradingIntent,
        risk: RiskTolerance
    ): FundamentalsNewsSynthesis {
        if (setups.isEmpty()) return FundamentalsNewsSynthesis()

        val setupData = setups.joinToString("\n\n") { setup ->
            val reasons = if (setup.reasons.isEmpty()) "None" else setup.reasons.joinToString("; ")
            val profile = setup.profile?.let {
                "Company: ${it.name}\nSector: ${it.sector ?: "N/A"}\nIndustry: ${it.industry ?: "N/A"}"
            } ?: "Company: N/A"
            val metrics = setup.metrics?.let { metrics ->
                listOfNotNull(
                    metrics.marketCap?.let { "Market Cap: $it" },
                    metrics.peRatio?.let { "PE: $it" },
                    metrics.eps?.let { "EPS: $it" },
                    metrics.earningsDate?.let { "Earnings: $it" },
                    metrics.dividendYield?.let { "Dividend Yield: $it" }
                ).joinToString("\n").ifBlank { "Metrics: N/A" }
            } ?: "Metrics: N/A"
            val sentiment = setup.sentiment?.let { "Sentiment: ${it.label ?: "N/A"} (${it.score ?: "N/A"})" } ?: "Sentiment: N/A"

            "Symbol: ${setup.symbol}\nConfidence: ${String.format(Locale.US, "%.2f", setup.confidence)}\nSetup Type: ${setup.setupType}\n$profile\n$metrics\n$sentiment\nReasons: $reasons"
        }

        val rssDigestText = rssDigest?.itemsBySymbol?.entries?.joinToString("\n\n") { (symbol, headlines) ->
            val headlineText = if (headlines.isEmpty()) {
                "- No recent headlines."
            } else {
                headlines.joinToString("\n") { headline ->
                    "- ${headline.title} (${headline.publishedAt})"
                }
            }
            "$symbol:\n$headlineText"
        } ?: "No recent headlines."

        val prompt = AiPrompts.FUNDAMENTALS_NEWS_SYNTHESIS_PROMPT
            .replace("{intent}", intent.name)
            .replace("{risk}", risk.name)
            .replace("{setupData}", setupData)
            .replace("{rssDigest}", rssDigestText)
            .trimIndent()

        val startTime = System.currentTimeMillis()
        return try {
            val request = LlmStageRequest(
                systemPrompt = AiPrompts.SYSTEM_ANALYST,
                userPrompt = prompt,
                stage = AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS,
                expectedSchemaId = "FundamentalsNewsSynthesis"
            )
            val response = stageModelRouter.run(AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS, request)
            val duration = System.currentTimeMillis() - startTime
            com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("FundamentalsNewsSynthesis", prompt, response.rawText, true, duration)

            val json = response.parsedJson ?: JsonExtraction.extractFirstJsonObject(response.rawText)
            if (json == null) {
                Logger.w("SynthesizeFundamentalsAndNewsUseCase", "No JSON found in LLM response")
                FundamentalsNewsSynthesis()
            } else {
                FundamentalsNewsSynthesis.fromJson(json)
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("FundamentalsNewsSynthesis", prompt, e.message ?: "Error", false, duration)
            Logger.e("SynthesizeFundamentalsAndNewsUseCase", "Fundamentals/news synthesis failed", e)
            FundamentalsNewsSynthesis()
        }
    }
}
