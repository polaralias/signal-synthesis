package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.*
import com.polaralias.signalsynthesis.domain.model.*
import com.polaralias.signalsynthesis.util.JsonExtraction
import com.polaralias.signalsynthesis.util.Logger

/**
 * Use case that uses an LLM to shortlist a subset of tradeable symbols for deeper analysis.
 * This acts as a 'gate' to reduce API calls for full enrichment.
 */
class ShortlistCandidatesUseCase(
    private val stageModelRouter: StageModelRouter
) {
    /**
     * Executes the shortlist logic.
     * 
     * @param symbols List of symbols that are considered tradeable.
     * @param quotes Map of symbols to their current quotes (price, volume, etc).
     * @param intent User's trading intent (DAY_TRADE, SWING, LONG_TERM).
     * @param risk User's risk tolerance.
     * @param llmKey API key for the LLM.
     * @param maxShortlist Maximum number of symbols to include in the shortlist.
     * @return ShortlistPlan containing the selected symbols and reasons.
     */
    suspend fun execute(
        symbols: List<String>,
        quotes: Map<String, Quote>,
        intent: TradingIntent,
        risk: RiskTolerance,
        maxShortlist: Int = 15
    ): ShortlistPlan {
        if (symbols.isEmpty()) {
            return ShortlistPlan()
        }

        // Build a concise data string for the LLM
        val quotesData = symbols.mapNotNull { symbol ->
            quotes[symbol]?.let { quote ->
                "$symbol: Price=${quote.price}, Change=${quote.changePercent}%, Vol=${quote.volume}"
            }
        }.joinToString("\n")

        val prompt = AiPrompts.SHORTLIST_PROMPT
            .replace("{intent}", intent.name)
            .replace("{risk}", risk.name)
            .replace("{quotesData}", quotesData)
            .replace("{maxShortlist}", maxShortlist.toString())
            .replace("{constraints}", "Select at most $maxShortlist symbols.")
            .trimIndent()

        Logger.i("ShortlistCandidatesUseCase", "Requesting shortlist for ${symbols.size} symbols")

        val startTime = System.currentTimeMillis()
        return try {
            val request = LlmStageRequest(
                systemPrompt = AiPrompts.SYSTEM_ANALYST,
                userPrompt = prompt,
                stage = AnalysisStage.SHORTLIST,
                expectedSchemaId = "ShortlistPlan"
            )
            
            val response = stageModelRouter.run(AnalysisStage.SHORTLIST, request)
            val duration = System.currentTimeMillis() - startTime
            com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("Shortlist", prompt, response.rawText, true, duration)
            
            val json = response.parsedJson ?: JsonExtraction.extractFirstJsonObject(response.rawText)
            if (json == null) {
                Logger.w("ShortlistCandidatesUseCase", "No JSON found in LLM response")
                return ShortlistPlan()
            }
            
            ShortlistPlan.fromJson(json)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("Shortlist", prompt, e.message ?: "Error", false, duration)
            Logger.e("ShortlistCandidatesUseCase", "Failed to get shortlist", e)
            ShortlistPlan()
        }
    }
}
