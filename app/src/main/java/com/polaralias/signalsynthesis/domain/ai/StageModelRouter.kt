package com.polaralias.signalsynthesis.domain.ai

import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.util.Logger

class StageModelRouter(
    private val runnerFactory: (LlmProvider, String, String) -> StageLlmRunner,
    private val routingConfigProvider: () -> UserModelRoutingConfig,
    private val apiKeysProvider: () -> Map<LlmProvider, String>
) {
    suspend fun run(stage: AnalysisStage, request: LlmStageRequest): LlmStageResponse {
        val routingConfig = routingConfigProvider()
        val stageConfig = routingConfig.getConfigForStage(stage)
        val provider = stageConfig.provider

        val finalToolsMode = when (stage) {
            AnalysisStage.DEEP_DIVE -> when {
                !provider.supportsWebTools() -> ToolsMode.NONE
                provider == LlmProvider.GEMINI && stageConfig.tools == ToolsMode.WEB_SEARCH -> ToolsMode.GOOGLE_SEARCH
                provider == LlmProvider.OPENAI && stageConfig.tools == ToolsMode.GOOGLE_SEARCH -> ToolsMode.WEB_SEARCH
                else -> stageConfig.tools
            }
            else -> ToolsMode.NONE
        }

        // Prioritize user stage config over request defaults for execution params.
        val finalRequest = request.copy(
            stage = stage,
            toolsMode = finalToolsMode,
            maxOutputTokens = stageConfig.maxOutputTokens,
            timeoutMs = stageConfig.timeoutMs,
            temperature = stageConfig.temperature,
            reasoningDepth = stageConfig.reasoningDepth
        )

        val model = LlmModel.normalizeModelIdAlias(stageConfig.model)
        val apiKey = apiKeysProvider()[provider] ?: ""

        if (apiKey.isBlank() && provider.requiresApiKey) {
            Logger.e("StageModelRouter", "API Key missing for provider $provider at stage $stage")
        }

        val runner = runnerFactory(provider, model, apiKey)

        Logger.i(
            "StageModelRouter",
            "Routing stage $stage to ${provider.name}/$model tools=$finalToolsMode depth=${stageConfig.reasoningDepth}"
        )

        return try {
            runner.run(finalRequest)
        } catch (e: Exception) {
            Logger.e("StageModelRouter", "Stage $stage failed on ${provider.name}/$model: ${e.message}", e)
            throw e
        }
    }
}
