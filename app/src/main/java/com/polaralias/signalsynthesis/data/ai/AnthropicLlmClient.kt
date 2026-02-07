package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.ai.OutputLength
import com.polaralias.signalsynthesis.domain.ai.ReasoningDepth
import com.polaralias.signalsynthesis.domain.ai.Verbosity

class AnthropicLlmClient(
    private val service: AnthropicService,
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

        val request = AnthropicRequest(
            model = model,
            maxTokens = maxTokens,
            system = systemPrompt,
            messages = listOf(AnthropicMessage(role = "user", content = prompt))
        )

        val response = service.createMessage(apiKey = apiKey, request = request)
        return response.content
            .asSequence()
            .mapNotNull { it.text }
            .joinToString("")
    }
}
