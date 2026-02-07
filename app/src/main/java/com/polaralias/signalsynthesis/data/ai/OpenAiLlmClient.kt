package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.ai.LlmModel
import com.polaralias.signalsynthesis.domain.ai.OutputLength
import com.polaralias.signalsynthesis.domain.ai.ReasoningDepth
import com.polaralias.signalsynthesis.domain.ai.Verbosity

class OpenAiLlmClient(
    private val responsesService: OpenAiResponsesService,
    private val model: String = DEFAULT_MODEL
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

        val request = OpenAiResponseRequest(
            model = model,
            instructions = systemPrompt ?: com.polaralias.signalsynthesis.domain.ai.AiPrompts.SYSTEM_ANALYST,
            input = listOf(OpenAiInputMessage(role = ROLE_USER, content = prompt)),
            maxOutputTokens = maxTokens,
            reasoning = mapReasoningEffort(reasoningDepth)?.let { OpenAiReasoning(effort = it) },
            temperature = if (LlmModel.supportsCustomTemperature(model)) 0.2f else null
        )
        val response = responsesService.createResponse("Bearer $apiKey", request)
        return response.extractText()
    }

    private fun mapReasoningEffort(depth: ReasoningDepth): String? {
        if (!LlmModel.isReasoningFamily(model)) {
            return null
        }
        return when (depth) {
            ReasoningDepth.NONE, ReasoningDepth.MINIMAL -> "minimal"
            ReasoningDepth.LOW -> "low"
            ReasoningDepth.MEDIUM -> "medium"
            ReasoningDepth.HIGH, ReasoningDepth.EXTRA -> "high"
        }
    }

    companion object {
        private const val DEFAULT_MODEL = "gpt-5.1"
        private const val ROLE_USER = "user"
    }
}
