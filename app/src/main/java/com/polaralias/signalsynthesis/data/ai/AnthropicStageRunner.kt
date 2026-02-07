package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmStageRequest
import com.polaralias.signalsynthesis.domain.ai.LlmStageResponse
import com.polaralias.signalsynthesis.domain.ai.StageLlmRunner

class AnthropicStageRunner(
    private val service: AnthropicService,
    private val model: String,
    private val apiKey: String
) : StageLlmRunner {

    override suspend fun run(request: LlmStageRequest): LlmStageResponse {
        val anthropicRequest = AnthropicRequest(
            model = model,
            maxTokens = request.maxOutputTokens,
            system = request.systemPrompt,
            messages = listOf(AnthropicMessage(role = "user", content = request.userPrompt)),
            temperature = request.temperature
        )

        val response = service.createMessage(
            apiKey = apiKey,
            request = anthropicRequest
        )

        val text = response.content
            .asSequence()
            .mapNotNull { it.text }
            .joinToString("")

        return LlmStageResponse(
            rawText = text,
            parsedJson = if (request.expectedSchemaId != null) extractJson(text) else null,
            providerDebug = "model=$model, api=anthropic"
        )
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
