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

        val level = if (model.contains("flash")) {
            when (reasoningDepth) {
                ReasoningDepth.NONE, ReasoningDepth.MINIMAL -> "minimal"
                ReasoningDepth.LOW -> "low"
                ReasoningDepth.MEDIUM -> "medium"
                ReasoningDepth.HIGH, ReasoningDepth.EXTRA -> "high"
            }
        } else {
            // Pro model (only low and high supported)
            when (reasoningDepth) {
                ReasoningDepth.NONE, ReasoningDepth.MINIMAL, ReasoningDepth.LOW -> "low"
                ReasoningDepth.MEDIUM, ReasoningDepth.HIGH, ReasoningDepth.EXTRA -> "high"
            }
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
                    ReasoningDepth.NONE -> 0
                    ReasoningDepth.MINIMAL -> 250
                    ReasoningDepth.LOW -> 500
                    ReasoningDepth.MEDIUM -> 1000
                    ReasoningDepth.HIGH -> 2000
                    ReasoningDepth.EXTRA -> 4000
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
