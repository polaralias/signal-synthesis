package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmModel
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.LlmStageRequest
import com.polaralias.signalsynthesis.domain.ai.LlmStageResponse
import com.polaralias.signalsynthesis.domain.ai.StageLlmRunner

class OpenAiCompatibleStageRunner(
    private val service: OpenAiCompatibleService,
    private val provider: LlmProvider,
    private val model: String,
    private val apiKey: String
) : StageLlmRunner {

    override suspend fun run(request: LlmStageRequest): LlmStageResponse {
        val normalizedModel = LlmModel.normalizeModelIdAlias(model)
        val endpoint = chatEndpoint(provider.baseUrl)

        val chatRequest = OpenAiChatRequest(
            model = normalizedModel,
            messages = listOf(
                OpenAiMessage(role = "system", content = request.systemPrompt),
                OpenAiMessage(role = "user", content = request.userPrompt)
            ),
            maxCompletionTokens = if (provider == LlmProvider.MINIMAX) request.maxOutputTokens else null,
            maxTokens = if (provider == LlmProvider.MINIMAX) null else request.maxOutputTokens,
            temperature = request.temperature
        )

        val authorization = if (apiKey.isBlank()) {
            null
        } else {
            "Bearer $apiKey"
        }

        val response = service.createChatCompletion(endpoint, authorization, chatRequest)
        val text = response.choices.firstOrNull()?.message?.content.orEmpty()

        return LlmStageResponse(
            rawText = text,
            parsedJson = if (request.expectedSchemaId != null) extractJson(text) else null,
            providerDebug = "model=$normalizedModel, api=openai-compatible, provider=${provider.providerId}"
        )
    }

    private fun chatEndpoint(baseUrl: String): String {
        val normalized = baseUrl.trimEnd('/')
        return if (normalized.endsWith("/v1")) {
            "$normalized/chat/completions"
        } else {
            "$normalized/v1/chat/completions"
        }
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
