package com.polaralias.signalsynthesis.domain.ai

import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import org.json.JSONObject

data class StageModelConfig(
    val provider: LlmProvider,
    val model: String,
    val tools: ToolsMode = ToolsMode.NONE,
    val temperature: Float = 0.2f,
    val maxOutputTokens: Int = 2000,
    val timeoutMs: Long = 30_000L,
    val reasoningDepth: ReasoningDepth = ReasoningDepth.MEDIUM
)

data class UserModelRoutingConfig(
    val byStage: Map<AnalysisStage, StageModelConfig> = emptyMap()
) {
    fun getConfigForStage(stage: AnalysisStage): StageModelConfig {
        return byStage[stage] ?: getDefaultConfig(stage)
    }

    fun toJson(): String {
        val root = JSONObject()
        byStage.forEach { (stage, config) ->
            val stageObj = JSONObject()
            stageObj.put("provider", config.provider.name)
            stageObj.put("model", config.model)
            stageObj.put("tools", config.tools.name)
            stageObj.put("temperature", config.temperature.toDouble())
            stageObj.put("maxOutputTokens", config.maxOutputTokens)
            stageObj.put("timeoutMs", config.timeoutMs)
            stageObj.put("reasoningDepth", config.reasoningDepth.name)
            root.put(stage.name, stageObj)
        }
        return root.toString()
    }

    companion object {
        fun fromJson(json: String?): UserModelRoutingConfig {
            if (json.isNullOrBlank()) return UserModelRoutingConfig()
            return try {
                val root = JSONObject(json)
                val map = mutableMapOf<AnalysisStage, StageModelConfig>()
                AnalysisStage.values().forEach { stage ->
                    if (root.has(stage.name)) {
                        val stageObj = root.getJSONObject(stage.name)
                        map[stage] = StageModelConfig(
                            provider = LlmProvider.valueOf(stageObj.getString("provider")),
                            model = stageObj.getString("model"),
                            tools = ToolsMode.valueOf(stageObj.optString("tools", "NONE")),
                            temperature = stageObj.optDouble("temperature", 0.2).toFloat(),
                            maxOutputTokens = stageObj.optInt("maxOutputTokens", 2000),
                            timeoutMs = stageObj.optLong("timeoutMs", 30000L),
                            reasoningDepth = ReasoningDepth.valueOf(stageObj.optString("reasoningDepth", "MEDIUM"))
                        )
                    }
                }
                UserModelRoutingConfig(map)
            } catch (e: Exception) {
                UserModelRoutingConfig()
            }
        }

        fun getDefaultConfig(stage: AnalysisStage): StageModelConfig {
            return when (stage) {
                AnalysisStage.SHORTLIST -> StageModelConfig(
                    provider = LlmProvider.OPENAI,
                    model = "gpt-5.2",
                    temperature = 0.2f,
                    reasoningDepth = ReasoningDepth.MEDIUM,
                    maxOutputTokens = 1000
                )
                AnalysisStage.DECISION_UPDATE -> StageModelConfig(
                    provider = LlmProvider.OPENAI,
                    model = "gpt-5.2",
                    temperature = 0.2f,
                    reasoningDepth = ReasoningDepth.HIGH,
                    maxOutputTokens = 1500
                )
                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS -> StageModelConfig(
                    provider = LlmProvider.OPENAI,
                    model = "gpt-5.2",
                    temperature = 0.3f,
                    reasoningDepth = ReasoningDepth.HIGH,
                    maxOutputTokens = 2000
                )
                AnalysisStage.DEEP_DIVE -> StageModelConfig(
                    provider = LlmProvider.OPENAI,
                    model = "gpt-5.2",
                    tools = ToolsMode.WEB_SEARCH,
                    temperature = 0.2f,
                    reasoningDepth = ReasoningDepth.HIGH,
                    maxOutputTokens = 2000,
                    timeoutMs = 60_000L
                )
                AnalysisStage.RSS_VERIFY -> StageModelConfig(
                    provider = LlmProvider.OPENAI,
                    model = "gpt-5-mini",
                    tools = ToolsMode.NONE,
                    temperature = 0.1f,
                    reasoningDepth = ReasoningDepth.MINIMAL,
                    maxOutputTokens = 500
                )
            }
        }
    }
}
