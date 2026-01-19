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
            maxOutputTokens = when (outputLength) {
                OutputLength.SHORT -> 400
                OutputLength.STANDARD -> 800
                OutputLength.FULL -> 1500
            },
            reasoning = OpenAiReasoning(
                effort = when (reasoningDepth) {
                    ReasoningDepth.FAST -> "low"
                    ReasoningDepth.BALANCED -> "medium"
                    ReasoningDepth.DEEP -> "high"
                    ReasoningDepth.EXTRA -> "xhigh"
                }
            ),
            text = OpenAiText(
                verbosity = when (verbosity) {
                    Verbosity.LOW -> "low"
                    Verbosity.MEDIUM -> "medium"
                    Verbosity.HIGH -> "high"
                }
            )
        )
        val response = service.createChatCompletion("Bearer $apiKey", request)
        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }

    companion object {
        private const val DEFAULT_MODEL = "gpt-5.1"
        private const val ROLE_SYSTEM = "system"
        private const val ROLE_USER = "user"
    }
}
