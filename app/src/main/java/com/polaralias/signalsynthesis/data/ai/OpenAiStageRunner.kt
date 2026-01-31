package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.*
import com.polaralias.signalsynthesis.util.Logger
import kotlinx.coroutines.withTimeout

class OpenAiStageRunner(
    private val service: OpenAiService,
    private val responsesService: OpenAiResponsesService,
    private val model: String,
    private val apiKey: String
) : StageLlmRunner {

    override suspend fun run(request: LlmStageRequest): LlmStageResponse {
        return withTimeout(request.timeoutMs) {
            if (request.toolsMode == ToolsMode.WEB_SEARCH) {
                runWithWebSearch(request)
            } else {
                runStandard(request)
            }
        }
    }

    private suspend fun runStandard(request: LlmStageRequest): LlmStageResponse {
        val openAiRequest = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = request.systemPrompt),
                OpenAiMessage(role = "user", content = request.userPrompt)
            ),
            maxCompletionTokens = request.maxOutputTokens, // Using max_completion_tokens for o1/newer models
            reasoningEffort = mapReasoningEffort(request.reasoningDepth),
            temperature = request.temperature,
            responseFormat = if (request.expectedSchemaId != null) {
                 OpenAiResponseFormat(type = "json_object")
            } else null
        )

        val response = service.createChatCompletion("Bearer $apiKey", openAiRequest)
        val text = response.choices.firstOrNull()?.message?.content.orEmpty()
        
        return LlmStageResponse(
            rawText = text,
            parsedJson = if (request.expectedSchemaId != null) extractJson(text) else null,
            providerDebug = "model=$model, depth=${request.reasoningDepth}"
        )
    }

    private suspend fun runWithWebSearch(request: LlmStageRequest): LlmStageResponse {
        val openAiRequest = OpenAiResponseRequest(
            model = model,
            input = "${request.systemPrompt}\n\n${request.userPrompt}",
            tools = listOf(OpenAiTool(type = "web_search")),
            include = listOf("tool_calls"),
            reasoningEffort = mapReasoningEffort(request.reasoningDepth),
            temperature = request.temperature
        )

        val response = responsesService.createResponse("Bearer $apiKey", openAiRequest)
        val text = response.output?.text.orEmpty()
        
        val sources = response.output?.toolCalls?.flatMap { call ->
            call.webSearchCall?.result?.sources?.map { source ->
                LlmSource(
                    title = source.title ?: "Untitled",
                    url = source.url ?: "",
                    snippet = null
                )
            } ?: emptyList()
        } ?: emptyList()

        return LlmStageResponse(
            rawText = text,
            parsedJson = if (request.expectedSchemaId != null) extractJson(text) else null,
            sources = sources,
            providerDebug = "model=$model, tool=web_search, depth=${request.reasoningDepth}"
        )
    }

    private fun mapReasoningEffort(depth: ReasoningDepth): String? {
        // Only newer models support this. We assume the caller (StageModelRouter) has selected a compatible model.
        // Or we could check 'model' here. But typically API ignores unknown params or we should be safe.
        // However, for o1 models, effort is required/supported.
            if (model.startsWith("o1") || model.startsWith("gpt-5")) {
             return when (depth) {
                ReasoningDepth.NONE -> "minimal"
                ReasoningDepth.MINIMAL -> "minimal"
                ReasoningDepth.LOW -> "low"
                ReasoningDepth.MEDIUM -> "medium"
                ReasoningDepth.HIGH -> "high"
                ReasoningDepth.EXTRA -> "xhigh"
                else -> "medium"
            }
        }
        return null
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
