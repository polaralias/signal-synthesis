package com.polaralias.signalsynthesis.domain.ai

import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.util.Logger

class StageModelRouter(
    private val openAiRunnerFactory: (String, String) -> StageLlmRunner,
    private val geminiRunnerFactory: (String, String) -> StageLlmRunner,
    private val routingConfigProvider: () -> UserModelRoutingConfig,
    private val apiKeysProvider: () -> Map<LlmProvider, String>
) {
    suspend fun run(stage: AnalysisStage, request: LlmStageRequest): LlmStageResponse {
        val routingConfig = routingConfigProvider()
        val stageConfig = routingConfig.getConfigForStage(stage)
        val provider = stageConfig.provider
        
        // Apply guardrails
        val finalToolsMode = when (stage) {
            AnalysisStage.DEEP_DIVE -> {
                if (provider == LlmProvider.GEMINI && stageConfig.tools == ToolsMode.WEB_SEARCH) {
                    ToolsMode.GOOGLE_SEARCH
                } else {
                    stageConfig.tools
                }
            }
            else -> ToolsMode.NONE // No tools for other stages as per requirements
        }
        
        // Prioritize User Stage Config over Request defaults for execution params
        val finalRequest = request.copy(
            stage = stage,
            toolsMode = finalToolsMode,
            maxOutputTokens = stageConfig.maxOutputTokens, // User config controls output limit
            timeoutMs = stageConfig.timeoutMs,
            temperature = stageConfig.temperature,
            reasoningDepth = stageConfig.reasoningDepth
        )
        
        val model = stageConfig.model
        val apiKey = apiKeysProvider()[provider] ?: ""
        
        if (apiKey.isBlank()) {
            Logger.e("StageModelRouter", "API Key missing for provider $provider at stage $stage")
        }

        val runner = when (provider) {
            LlmProvider.OPENAI -> openAiRunnerFactory(model, apiKey)
            LlmProvider.GEMINI -> geminiRunnerFactory(model, apiKey)
        }
        
        Logger.i("StageModelRouter", "Routing stage $stage to $provider/$model tools=$finalToolsMode depth=${stageConfig.reasoningDepth}")
        
        return try {
            runner.run(finalRequest)
        } catch (e: Exception) {
            Logger.e("StageModelRouter", "Stage $stage failed on $provider/$model: ${e.message}", e)
            throw e
        }
    }
}
