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

        val authHeader = if (provider.requiresApiKey && apiKey.isNotBlank()) {
            "Bearer $apiKey"
        } else if (!provider.requiresApiKey && apiKey.isNotBlank()) {
            "Bearer $apiKey" // allow override even if not strictly required
        } else if (provider == LlmProvider.OLLAMA) {
            "Bearer ollama"
        } else if (provider == LlmProvider.SGLANG) {
            "Bearer EMPTY"
        } else {
            null
        }

        if (provider.supportsResponsesEndpoint) {
            val endpoint = if (provider.baseUrl.trimEnd('/').endsWith("/v1")) {
                "${provider.baseUrl.trimEnd('/')}/responses"
            } else {
                "${provider.baseUrl.trimEnd('/')}/v1/responses"
            }

            val request = OpenAiResponseRequest(
                model = model,
                input = listOf(
                    OpenAiInputMessage(role = "system", content = systemPrompt ?: com.polaralias.signalsynthesis.domain.ai.AiPrompts.SYSTEM_ANALYST),
                    OpenAiInputMessage(role = "user", content = prompt)
                ),
                maxOutputTokens = maxTokens,
                temperature = 0.2f
            )

            val response = service.createResponse(
                url = endpoint,
                authorization = authHeader,
                request = request
            )

            return response.extractText()
        }

        val request = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = systemPrompt ?: com.polaralias.signalsynthesis.domain.ai.AiPrompts.SYSTEM_ANALYST),
                OpenAiMessage(role = "user", content = prompt)
            ),
            maxCompletionTokens = if (provider == LlmProvider.MINIMAX) maxTokens else null,
            maxTokens = if (provider != LlmProvider.MINIMAX) maxTokens else null,
            temperature = 0.2f,
            topP = 0.95f,
            topK = if (provider.supportsTopK) 50 else null,
            enableThinking = if (provider == LlmProvider.SILICONFLOW) true else null,
            chatTemplateKwargs = if (provider.supportsNativeThinkingControl && (provider == LlmProvider.VLLM || provider == LlmProvider.SGLANG)) {
                mapOf("enable_thinking" to true)
            } else null,
            stream = false
        )

        val endpoint = if (provider.baseUrl.trimEnd('/').endsWith("/v1")) {
            "${provider.baseUrl.trimEnd('/')}/chat/completions"
        } else {
            "${provider.baseUrl.trimEnd('/')}/v1/chat/completions"
        }

        val response = service.createChatCompletion(
            url = endpoint,
            authorization = authHeader,
            request = request
        )

        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }
}
