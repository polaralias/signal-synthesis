package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.*
import com.polaralias.signalsynthesis.util.Logger

class GeminiStageRunner(
    private val service: GeminiService,
    private val model: String,
    private val apiKey: String
) : StageLlmRunner {

    override suspend fun run(request: LlmStageRequest): LlmStageResponse {
        val tools = if (request.toolsMode == ToolsMode.GOOGLE_SEARCH) {
            listOf(GeminiTool(googleSearch = GoogleSearchTool()))
        } else null

        val systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = request.systemPrompt)))
        
        val userContent = GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(text = request.userPrompt))
        )

        // Only include thinking_level for Gemini 3 models (and check provider specifics if needed)
        // For simplicity, we assume the router sends us the right model string (e.g. gemini-3-flash) which supports it.
        val thinkingLevel = mapThinkingLevel(request.reasoningDepth)

        val genConfig = GeminiGenerationConfig(
            temperature = request.temperature?.toDouble() ?: 0.2,
            maxOutputTokens = request.maxOutputTokens,
            thinkingLevel = thinkingLevel,
            // We could also map thinking_budget based on depth if we wanted to enforce token limits logic,
            // but the docs only requested thinking_level mapping.
            thinkingBudget = null 
        )

        val geminiRequest = GeminiRequest(
            contents = listOf(userContent),
            systemInstruction = systemInstruction,
            tools = tools,
            generationConfig = genConfig
        )

        val response = service.generateContent(model, apiKey, geminiRequest)
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
            providerDebug = "model=$model, depth=${request.reasoningDepth}, level=$thinkingLevel"
        )
    }

    private fun mapThinkingLevel(depth: ReasoningDepth): String? {
        // If not a Gemini 3 model, this might be ignored by API or cause error.
        // Assuming we only use this for 3+ models as per user specs.
        if (!model.contains("gemini-3")) return null

        val isFlash = model.contains("flash")

        return when (depth) {
            ReasoningDepth.MINIMAL -> if (isFlash) "minimal" else "low" // Pro doesn't support minimal
            ReasoningDepth.LOW -> "low"
            ReasoningDepth.MEDIUM -> if (isFlash) "medium" else "high" // Pro doesn't support medium, upgrade to high
            ReasoningDepth.HIGH -> "high"
            ReasoningDepth.EXTRA -> "high"
            else -> "medium" // default
        }
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
