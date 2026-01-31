package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.AiPrompts
import com.polaralias.signalsynthesis.domain.ai.DeepDiveProvider
import com.polaralias.signalsynthesis.domain.model.DeepDive
import com.polaralias.signalsynthesis.domain.model.DeepDiveSource
import com.polaralias.signalsynthesis.domain.model.RssHeadline
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.util.ActivityLogger
import com.polaralias.signalsynthesis.util.Logger
import kotlinx.coroutines.withTimeout

class OpenAiDeepDiveProvider(
    private val openAiResponsesService: OpenAiResponsesService
) : DeepDiveProvider {

    override suspend fun deepDive(
        symbol: String,
        intent: TradingIntent,
        snapshot: String,
        rssHeadlines: List<RssHeadline>?,
        apiKey: String,
        model: String,
        timeoutMs: Long
    ): DeepDive {
        val rssHeadlinesText = rssHeadlines?.joinToString("\n") { "- ${it.title}" } ?: "No recent headlines found."
        
        val prompt = AiPrompts.DEEP_DIVE_PROMPT
            .replace("{symbol}", symbol)
            .replace("{intent}", intent.name)
            .replace("{snapshot}", snapshot)
            .replace("{rssHeadlines}", rssHeadlinesText)

        val request = OpenAiResponseRequest(
            model = model,
            input = prompt,
            tools = listOf(OpenAiTool(type = "web_search")),
            include = listOf("web_search_call.action.sources")
        )

        return try {
            withTimeout(timeoutMs) {
                val startTime = System.currentTimeMillis()
                val response = openAiResponsesService.createResponse(
                    authorization = "Bearer $apiKey",
                    request = request
                )
                val duration = System.currentTimeMillis() - startTime
                
                val outputText = response.output?.text
                val apiSources = response.output?.toolCalls?.flatMap { call ->
                    call.webSearchCall?.action?.sources?.map { source ->
                        DeepDiveSource(
                            title = source.title ?: "Link",
                            publisher = source.publisher ?: "",
                            publishedAt = source.publishedAt ?: "",
                            url = source.url ?: ""
                        )
                    } ?: emptyList()
                } ?: emptyList()

                if (outputText == null) {
                    Logger.w("OpenAiDeepDiveProvider", "OpenAI returned null output text for $symbol")
                    return@withTimeout deterministicNoActionableNews()
                }

                val deepDive = DeepDive.fromJson(extractJson(outputText))
                
                // Combine sources from LLM JSON and API tool call metadata
                val combinedSources = (deepDive.sources + apiSources).distinctBy { it.url }
                
                ActivityLogger.logLlm("DeepDive", prompt, outputText, true, duration, provider = "OpenAI")
                
                deepDive.copy(sources = combinedSources)
            }
        } catch (e: Exception) {
            Logger.e("OpenAiDeepDiveProvider", "Failed for $symbol: ${e.message}", e)
            deterministicNoActionableNews()
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
