package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.*

class GeminiStageRunner(
    private val service: GeminiService,
    private val model: String,
    private val apiKey: String
) : StageLlmRunner {

    override suspend fun run(request: LlmStageRequest): LlmStageResponse {
        val normalizedModel = LlmModel.normalizeModelIdAlias(model)
        val tools = if (request.toolsMode == ToolsMode.GOOGLE_SEARCH) {
            listOf(GeminiTool(googleSearch = GoogleSearchTool()))
        } else {
            null
        }

        val systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = request.systemPrompt)))
        val userContent = GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(text = request.userPrompt))
        )

        val thinkingLevel = mapThinkingLevel(request.reasoningDepth, normalizedModel)

        val genConfig = GeminiGenerationConfig(
            temperature = request.temperature?.toDouble() ?: 0.2,
            maxOutputTokens = request.maxOutputTokens,
            thinkingLevel = thinkingLevel
        )

        val geminiRequest = GeminiRequest(
            contents = listOf(userContent),
            systemInstruction = systemInstruction,
            tools = tools,
            generationConfig = genConfig
        )

        val response = service.generateContent(
            apiVersion = geminiApiVersionForModel(normalizedModel),
            model = normalizedModel,
            apiKey = apiKey,
            request = geminiRequest
        )
        val candidate = response.candidates.firstOrNull()

        val text = candidate?.content?.parts?.joinToString("") { it.text ?: "" } ?: ""

        val sources = candidate?.groundingMetadata?.groundingChunks?.mapNotNull { chunk ->
            chunk.web?.let { web ->
                LlmSource(
                    title = web.title ?: "Untitled",
                    url = web.uri ?: "",
                    snippet = null
                )
            }
        } ?: emptyList()

        return LlmStageResponse(
            rawText = text,
            parsedJson = if (request.expectedSchemaId != null) extractJson(text) else null,
            sources = sources,
            providerDebug = "model=$normalizedModel, depth=${request.reasoningDepth}, level=$thinkingLevel"
        )
    }

    private fun mapThinkingLevel(depth: ReasoningDepth, modelId: String): String? {
        val normalized = modelId.lowercase()
        if (!normalized.startsWith("gemini-3")) return null

        val isFlash = normalized.contains("flash")
        return when (depth) {
            ReasoningDepth.NONE, ReasoningDepth.MINIMAL -> if (isFlash) "minimal" else "low"
            ReasoningDepth.LOW -> "low"
            ReasoningDepth.MEDIUM -> if (isFlash) "medium" else "high"
            ReasoningDepth.HIGH, ReasoningDepth.EXTRA -> "high"
        }
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
