package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.ai.ReasoningDepth
import com.polaralias.signalsynthesis.domain.ai.OutputLength
import com.polaralias.signalsynthesis.domain.ai.Verbosity

class OpenAiLlmClient(
    private val service: OpenAiService,
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
        val request = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(
                    role = ROLE_SYSTEM,
                    content = systemPrompt ?: com.polaralias.signalsynthesis.domain.ai.AiPrompts.SYSTEM_ANALYST
                ),
                OpenAiMessage(
                    role = ROLE_USER,
                    content = prompt
                )
            ),
            maxCompletionTokens = when (outputLength) {
                OutputLength.SHORT -> 400
                OutputLength.STANDARD -> 800
                OutputLength.FULL -> 1500
            },
            reasoningEffort = mapReasoningEffort(reasoningDepth)
        )
        val response = service.createChatCompletion("Bearer $apiKey", request)
        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }

    private fun mapReasoningEffort(depth: ReasoningDepth): String? {
        if (model.startsWith("o1") || model.startsWith("gpt-5")) {
            return when (depth) {
                ReasoningDepth.NONE -> "minimal"
                ReasoningDepth.MINIMAL -> "minimal"
                ReasoningDepth.LOW -> "low"
                ReasoningDepth.MEDIUM -> "medium"
                ReasoningDepth.HIGH -> "high"
                ReasoningDepth.EXTRA -> "xhigh"
            }
        }
        return null
    }

    companion object {
        private const val DEFAULT_MODEL = "gpt-5.1"
        private const val ROLE_SYSTEM = "system"
        private const val ROLE_USER = "user"
    }
}
