package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.AiPrompts
import com.polaralias.signalsynthesis.domain.ai.LlmStageRequest
import com.polaralias.signalsynthesis.domain.ai.StageModelRouter
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.domain.model.DecisionUpdate
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.util.JsonExtraction
import com.polaralias.signalsynthesis.util.Logger

class UpdateDecisionsUseCase(
    private val stageModelRouter: StageModelRouter
) {
    suspend fun execute(
        setups: List<TradeSetup>,
        intent: TradingIntent,
        risk: RiskTolerance,
        maxKeep: Int = 10
    ): DecisionUpdate {
        if (setups.isEmpty()) return DecisionUpdate()

        val setupData = setups.joinToString("\n") { setup ->
            val reasons = if (setup.reasons.isEmpty()) "None" else setup.reasons.joinToString("; ")
            val levels = "Trigger ${formatPrice(setup.triggerPrice)}, Stop ${formatPrice(setup.stopLoss)}, Target ${formatPrice(setup.targetPrice)}"
            val profile = setup.profile?.let { "${it.name} (${it.sector ?: "N/A"})" } ?: "N/A"
            val metrics = setup.metrics?.let { metrics ->
                listOfNotNull(
                    metrics.marketCap?.let { "MarketCap=$it" },
                    metrics.peRatio?.let { "PE=$it" },
                    metrics.earningsDate?.let { "Earnings=$it" }
                ).joinToString(", ").ifBlank { "N/A" }
            } ?: "N/A"
            val sentiment = setup.sentiment?.let { "${it.label ?: "N/A"} (${it.score ?: "N/A"})" } ?: "N/A"

            "${setup.symbol} | ${setup.setupType} | confidence=${String.format("%.2f", setup.confidence)} | $levels | profile=$profile | metrics=$metrics | sentiment=$sentiment | reasons=$reasons"
        }

        val prompt = AiPrompts.DECISION_UPDATE_PROMPT
            .replace("{intent}", intent.name)
            .replace("{risk}", risk.name)
            .replace("{setupData}", setupData)
            .replace("{maxKeep}", maxKeep.toString())
            .trimIndent()

        val startTime = System.currentTimeMillis()
        return try {
            val request = LlmStageRequest(
                systemPrompt = AiPrompts.SYSTEM_ANALYST,
                userPrompt = prompt,
                stage = AnalysisStage.DECISION_UPDATE,
                expectedSchemaId = "DecisionUpdate"
            )
            val response = stageModelRouter.run(AnalysisStage.DECISION_UPDATE, request)
            val duration = System.currentTimeMillis() - startTime
            com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("DecisionUpdate", prompt, response.rawText, true, duration)

            val json = response.parsedJson ?: JsonExtraction.extractFirstJsonObject(response.rawText)
            if (json == null) {
                Logger.w("UpdateDecisionsUseCase", "No JSON found in LLM response")
                DecisionUpdate()
            } else {
                DecisionUpdate.fromJson(json)
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("DecisionUpdate", prompt, e.message ?: "Error", false, duration)
            Logger.e("UpdateDecisionsUseCase", "Decision update failed", e)
            DecisionUpdate()
        }
    }

    private fun formatPrice(value: Double): String = String.format("%.2f", value)
}
