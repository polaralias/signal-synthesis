package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.*
import kotlinx.coroutines.withTimeout

class OpenAiStageRunner(
    private val responsesService: OpenAiResponsesService,
    private val model: String,
    private val apiKey: String
) : StageLlmRunner {

    override suspend fun run(request: LlmStageRequest): LlmStageResponse {
        return withTimeout(request.timeoutMs) {
            if (request.toolsMode == ToolsMode.WEB_SEARCH) {
                runWithWebSearchResponses(request)
            } else {
                runStandardResponses(request)
            }
        }
    }

    private suspend fun runStandardResponses(request: LlmStageRequest): LlmStageResponse {
        val openAiRequest = OpenAiResponseRequest(
            model = model,
            instructions = request.systemPrompt,
            input = listOf(OpenAiInputMessage(role = "user", content = request.userPrompt)),
            maxOutputTokens = request.maxOutputTokens,
            text = structuredTextConfig(request.expectedSchemaId),
            reasoning = mapReasoningEffort(request.reasoningDepth)?.let { OpenAiReasoning(it) },
            temperature = if (LlmModel.supportsCustomTemperature(model)) request.temperature else null
        )

        val response = responsesService.createResponse("Bearer $apiKey", openAiRequest)
        val text = response.extractText()

        return LlmStageResponse(
            rawText = text,
            parsedJson = parseStructuredJson(request.expectedSchemaId, text),
            providerDebug = "model=$model, api=responses, depth=${request.reasoningDepth}"
        )
    }

    private suspend fun runWithWebSearchResponses(request: LlmStageRequest): LlmStageResponse {
        val openAiRequest = OpenAiResponseRequest(
            model = model,
            instructions = request.systemPrompt,
            input = listOf(OpenAiInputMessage(role = "user", content = request.userPrompt)),
            maxOutputTokens = request.maxOutputTokens,
            text = structuredTextConfig(request.expectedSchemaId),
            tools = listOf(OpenAiTool(type = "web_search")),
            include = listOf("web_search_call.action.sources"),
            reasoning = mapReasoningEffort(request.reasoningDepth)?.let { OpenAiReasoning(it) },
            temperature = if (LlmModel.supportsCustomTemperature(model)) request.temperature else null
        )

        val response = responsesService.createResponse("Bearer $apiKey", openAiRequest)
        val text = response.extractText()

        val sources = response.extractSources().map { source ->
            LlmSource(
                title = source.title ?: "Untitled",
                url = source.url ?: "",
                snippet = null
            )
        }

        return LlmStageResponse(
            rawText = text,
            parsedJson = parseStructuredJson(request.expectedSchemaId, text),
            sources = sources,
            providerDebug = "model=$model, api=responses, tool=web_search, depth=${request.reasoningDepth}"
        )
    }

    private fun mapReasoningEffort(depth: ReasoningDepth): String? {
        return if (LlmModel.isReasoningFamily(model)) {
            when (depth) {
                ReasoningDepth.NONE, ReasoningDepth.MINIMAL -> "minimal"
                ReasoningDepth.LOW -> "low"
                ReasoningDepth.MEDIUM -> "medium"
                ReasoningDepth.HIGH, ReasoningDepth.EXTRA -> "high"
            }
        } else {
            null
        }
    }

    private fun structuredTextConfig(expectedSchemaId: String?): OpenAiTextConfig? {
        if (expectedSchemaId == null) return null
        return OpenAiTextConfig(
            format = OpenAiTextFormat(type = "json_object")
        )
    }

    private fun parseStructuredJson(expectedSchemaId: String?, text: String): String? {
        if (expectedSchemaId == null) return null
        val trimmed = text.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }
        return extractJson(text)
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
