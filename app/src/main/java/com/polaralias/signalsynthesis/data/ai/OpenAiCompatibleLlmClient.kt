package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.OutputLength
import com.polaralias.signalsynthesis.domain.ai.ReasoningDepth
import com.polaralias.signalsynthesis.domain.ai.Verbosity

class OpenAiCompatibleLlmClient(
    private val service: OpenAiCompatibleService,
    private val provider: LlmProvider,
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

        val request = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = systemPrompt ?: com.polaralias.signalsynthesis.domain.ai.AiPrompts.SYSTEM_ANALYST),
                OpenAiMessage(role = "user", content = prompt)
            ),
            maxCompletionTokens = if (provider == LlmProvider.MINIMAX) maxTokens else null,
            maxTokens = if (provider == LlmProvider.MINIMAX) null else maxTokens,
            temperature = 0.2f
        )

        val endpoint = if (provider.baseUrl.trimEnd('/').endsWith("/v1")) {
            "${provider.baseUrl.trimEnd('/')}/chat/completions"
        } else {
            "${provider.baseUrl.trimEnd('/')}/v1/chat/completions"
        }

        val response = service.createChatCompletion(
            url = endpoint,
            authorization = if (apiKey.isBlank()) null else "Bearer $apiKey",
            request = request
        )

        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }
}
