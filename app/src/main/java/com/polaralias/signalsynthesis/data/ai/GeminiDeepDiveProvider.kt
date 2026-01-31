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

class GeminiDeepDiveProvider(
    private val geminiService: GeminiService
) : DeepDiveProvider {

    override suspend fun deepDive(
        symbol: String,
        intent: TradingIntent,
        snapshot: String,
        rssHeadlinesList: List<RssHeadline>?,
        apiKey: String,
        model: String,
        timeoutMs: Long
    ): DeepDive {
        val rssHeadlines = rssHeadlinesList?.joinToString("\n") { "- ${it.title}" } ?: "No recent headlines found."
        
        val prompt = AiPrompts.DEEP_DIVE_PROMPT
            .replace("{symbol}", symbol)
            .replace("{intent}", intent.name)
            .replace("{snapshot}", snapshot)
            .replace("{rssHeadlines}", rssHeadlines)

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            tools = listOf(GeminiTool(googleSearch = GoogleSearchTool())),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json"
            )
        )

        return try {
            withTimeout(timeoutMs) {
                val startTime = System.currentTimeMillis()
                val response = geminiService.generateContent(
                    model = model,
                    apiKey = apiKey,
                    request = request
                )
                val duration = System.currentTimeMillis() - startTime
                
                val candidate = response.candidates.firstOrNull()
                val outputText = candidate?.content?.parts?.firstOrNull()?.text
                
                val groundingSources = candidate?.groundingMetadata?.groundingChunks?.mapNotNull { chunk ->
                    chunk.web?.let { web ->
                        DeepDiveSource(
                            title = web.title ?: "Link",
                            url = web.uri ?: "",
                            publisher = "", // Gemini doesn't always provide publisher in metadata
                            publishedAt = "" // Gemini doesn't always provide date in metadata
                        )
                    }
                } ?: emptyList()

                if (outputText == null) {
                    Logger.w("GeminiDeepDiveProvider", "Gemini returned null output text for $symbol")
                    return@withTimeout deterministicNoActionableNews()
                }

                val deepDive = DeepDive.fromJson(extractJson(outputText))
                
                // Merge sources, preferring grounding metadata as authoritative
                val combinedSources = (groundingSources + deepDive.sources).distinctBy { it.url }
                
                ActivityLogger.logLlm("DeepDive (Gemini)", prompt, outputText, true, duration)
                
                deepDive.copy(sources = combinedSources)
            }
        } catch (e: Exception) {
            Logger.e("GeminiDeepDiveProvider", "Failed for $symbol: ${e.message}", e)
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
