package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.ai.OutputLength
import com.polaralias.signalsynthesis.domain.ai.ReasoningDepth
import com.polaralias.signalsynthesis.domain.ai.Verbosity
import com.polaralias.signalsynthesis.domain.ai.LlmModel

class GeminiLlmClient(
    private val service: GeminiService,
    private val model: String
) : LlmClient {
    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        apiKey: String,
        reasoningDepth: ReasoningDepth,
        outputLength: OutputLength,
        verbosity: Verbosity
    ): String {
        val maxTokens = when (outputLength) {
            OutputLength.SHORT -> 400
            OutputLength.STANDARD -> 800
            OutputLength.FULL -> 1500
        }

        val normalizedModel = LlmModel.normalizeModelIdAlias(model)
        val isGemini3 = normalizedModel.lowercase().startsWith("gemini-3")
        val isFlash = normalizedModel.lowercase().contains("flash")

        val generationConfig = if (isGemini3) {
            GeminiGenerationConfig(
                maxOutputTokens = maxTokens,
                thinkingLevel = mapThinkingLevel(reasoningDepth, isFlash)
            )
        } else {
            GeminiGenerationConfig(
                maxOutputTokens = maxTokens,
                thinkingBudget = mapThinkingBudget(reasoningDepth)
            )
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(
                        text = systemPrompt ?: com.polaralias.signalsynthesis.domain.ai.AiPrompts.SYSTEM_ANALYST
                    )
                )
            ),
            generationConfig = generationConfig
        )

        val response = service.generateContent(
            apiVersion = geminiApiVersionForModel(normalizedModel),
            model = normalizedModel,
            apiKey = apiKey,
            request = request
        )

        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()
    }

    private fun mapThinkingLevel(depth: ReasoningDepth, isFlash: Boolean): String {
        return when (depth) {
            ReasoningDepth.NONE, ReasoningDepth.MINIMAL -> if (isFlash) "minimal" else "low"
            ReasoningDepth.LOW -> "low"
            ReasoningDepth.MEDIUM -> if (isFlash) "medium" else "high"
            ReasoningDepth.HIGH, ReasoningDepth.EXTRA -> "high"
        }
    }

    private fun mapThinkingBudget(depth: ReasoningDepth): Int {
        return when (depth) {
            ReasoningDepth.NONE -> 0
            ReasoningDepth.MINIMAL -> 250
            ReasoningDepth.LOW -> 500
            ReasoningDepth.MEDIUM -> 1000
            ReasoningDepth.HIGH -> 2000
            ReasoningDepth.EXTRA -> 4000
        }
    }
}
