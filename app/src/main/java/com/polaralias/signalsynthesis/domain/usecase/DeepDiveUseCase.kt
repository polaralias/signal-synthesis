package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.domain.ai.*
import com.polaralias.signalsynthesis.domain.model.DeepDive
import com.polaralias.signalsynthesis.domain.model.RssHeadline
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.domain.model.DeepDiveSource
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.util.Logger

class DeepDiveUseCase(
    private val stageModelRouter: StageModelRouter,
    private val openAiProvider: DeepDiveProvider? = null,
    private val geminiProvider: DeepDiveProvider? = null,
    private val routingConfigProvider: (() -> UserModelRoutingConfig)? = null,
    private val apiKeysProvider: (() -> Map<LlmProvider, String>)? = null
) {
    suspend fun execute(
        symbol: String,
        intent: TradingIntent,
        snapshot: String,
        rssHeadlinesList: List<RssHeadline>?,
        timeoutMs: Long = 45000L
    ): DeepDive {
        val rssHeadlines = rssHeadlinesList?.joinToString("\n") { "- ${it.title}" } ?: "No recent headlines found."

        // Prefer provider implementations when configured (includes tool-enabled deep dives).
        val routed = resolveProvider(timeoutMs)
        if (routed != null) {
            val (provider, model, apiKey, resolvedTimeout) = routed
            return try {
                provider.deepDive(
                    symbol = symbol,
                    intent = intent,
                    snapshot = snapshot,
                    rssHeadlines = rssHeadlinesList,
                    apiKey = apiKey,
                    model = model,
                    timeoutMs = resolvedTimeout
                )
            } catch (e: Exception) {
                Logger.e("DeepDiveUseCase", "Provider deep dive failed for $symbol: ${e.message}", e)
                deterministicNoActionableNews()
            }
        }
        
        val prompt = AiPrompts.DEEP_DIVE_PROMPT
            .replace("{symbol}", symbol)
            .replace("{intent}", intent.name)
            .replace("{snapshot}", snapshot)
            .replace("{rssHeadlines}", rssHeadlines)

        val request = LlmStageRequest(
            systemPrompt = AiPrompts.SYSTEM_ANALYST,
            userPrompt = prompt,
            stage = AnalysisStage.DEEP_DIVE,
            expectedSchemaId = "DeepDive",
            timeoutMs = timeoutMs,
            maxOutputTokens = 2000
        )

        return try {
            val response = stageModelRouter.run(AnalysisStage.DEEP_DIVE, request)
            
            val json = response.parsedJson ?: extractJson(response.rawText)
            if (json == null) {
                Logger.w("DeepDiveUseCase", "Gemini/OpenAI returned null output text for $symbol")
                return deterministicNoActionableNews()
            }

            val deepDive = DeepDive.fromJson(json)
            
            // Map LlmSource to DeepDiveSource
            val groundingSources = response.sources.map { source ->
                DeepDiveSource(
                    title = source.title,
                    url = source.url,
                    publisher = "",
                    publishedAt = ""
                )
            }
            
            // Merge sources, preferring grounding metadata as authoritative
            val combinedSources = (groundingSources + deepDive.sources).distinctBy { it.url }
            
            deepDive.copy(sources = combinedSources)
        } catch (e: Exception) {
            Logger.e("DeepDiveUseCase", "Failed for $symbol: ${e.message}", e)
            deterministicNoActionableNews()
        }
    }

    private data class RoutedProvider(
        val provider: DeepDiveProvider,
        val model: String,
        val apiKey: String,
        val timeoutMs: Long
    )

    private fun resolveProvider(fallbackTimeoutMs: Long): RoutedProvider? {
        val routingConfig = routingConfigProvider?.invoke() ?: return null
        val apiKeys = apiKeysProvider?.invoke() ?: return null
        val openAi = openAiProvider ?: return null
        val gemini = geminiProvider ?: return null

        val stageConfig = routingConfig.getConfigForStage(AnalysisStage.DEEP_DIVE)
        val provider = stageConfig.provider
        val apiKey = apiKeys[provider].orEmpty()
        if (apiKey.isBlank()) return null

        val toolsMode = if (provider == LlmProvider.GEMINI && stageConfig.tools == ToolsMode.WEB_SEARCH) {
            ToolsMode.GOOGLE_SEARCH
        } else {
            stageConfig.tools
        }
        if (toolsMode == ToolsMode.NONE) return null

        val model = stageConfig.model
        val resolvedTimeout = if (stageConfig.timeoutMs > 0) stageConfig.timeoutMs else fallbackTimeoutMs

        return when (provider) {
            LlmProvider.OPENAI -> RoutedProvider(openAi, model, apiKey, resolvedTimeout)
            LlmProvider.GEMINI -> RoutedProvider(gemini, model, apiKey, resolvedTimeout)
            else -> null
        }
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }

    private fun deterministicNoActionableNews(): DeepDive {
        return DeepDive(
            summary = "No actionable recent news found for this ticker within the last 72 hours that would significantly alter the current thesis.",
            drivers = emptyList(),
            risks = emptyList(),
            whatChangesMyMind = emptyList(),
            sources = emptyList()
        )
    }
}
