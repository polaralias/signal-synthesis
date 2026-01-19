package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.ai.ReasoningDepth
import com.polaralias.signalsynthesis.domain.ai.OutputLength
import com.polaralias.signalsynthesis.domain.ai.Verbosity

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

        val level = when (reasoningDepth) {
            ReasoningDepth.FAST -> "low"
            ReasoningDepth.BALANCED -> "medium"
            ReasoningDepth.DEEP -> "high"
            ReasoningDepth.EXTRA -> "high" // fall back to high
        }

        val generationConfig = if (model.contains("gemini-3")) {
            GeminiGenerationConfig(
                maxOutputTokens = maxTokens,
                thinkingLevel = level
            )
        } else {
            // Assume 2.5 or other if not 3
            GeminiGenerationConfig(
                maxOutputTokens = maxTokens,
                thinkingBudget = when (reasoningDepth) {
                    ReasoningDepth.FAST -> 500
                    ReasoningDepth.BALANCED -> 1000
                    ReasoningDepth.DEEP -> 2000
                    ReasoningDepth.EXTRA -> 2000
                }
            )
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            systemInstruction = systemPrompt?.let {
                GeminiContent(
                    parts = listOf(GeminiPart(text = it))
                )
            } ?: GeminiContent(
                parts = listOf(GeminiPart(text = com.polaralias.signalsynthesis.domain.ai.AiPrompts.SYSTEM_ANALYST)),
            ),
            generationConfig = generationConfig
        )
        
        val response = service.generateContent(
            model = model,
            apiKey = apiKey,
            request = request
        )
        
        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()
    }
}
