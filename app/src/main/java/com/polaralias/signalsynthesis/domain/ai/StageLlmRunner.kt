package com.polaralias.signalsynthesis.domain.ai

import com.polaralias.signalsynthesis.domain.model.AnalysisStage

enum class ToolsMode {
    NONE,
    WEB_SEARCH,
    GOOGLE_SEARCH
}

data class LlmStageRequest(
    val systemPrompt: String,
    val userPrompt: String,
    val stage: AnalysisStage,
    val expectedSchemaId: String? = null,
    val timeoutMs: Long = 30_000L,
    val maxOutputTokens: Int = 2000,
    val toolsMode: ToolsMode = ToolsMode.NONE,
    val temperature: Float? = null,
    val reasoningDepth: ReasoningDepth = ReasoningDepth.MEDIUM
)

data class LlmSource(
    val title: String,
    val url: String,
    val snippet: String? = null
)

data class LlmStageResponse(
    val rawText: String,
    val parsedJson: String? = null,
    val sources: List<LlmSource> = emptyList(),
    val providerDebug: String? = null
)

interface StageLlmRunner {
    suspend fun run(request: LlmStageRequest): LlmStageResponse
}
