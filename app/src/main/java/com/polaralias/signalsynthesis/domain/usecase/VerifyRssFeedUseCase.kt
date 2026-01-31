package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.rss.RssFeedClient
import com.polaralias.signalsynthesis.domain.ai.StageModelRouter
import com.polaralias.signalsynthesis.domain.ai.LlmStageRequest
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.util.Logger
import org.json.JSONObject

data class RssVerificationResult(
    val isValid: Boolean,
    val title: String,
    val description: String
)

class VerifyRssFeedUseCase(
    private val rssClient: RssFeedClient,
    private val stageModelRouter: StageModelRouter
) {
    suspend fun execute(url: String): RssVerificationResult {
        val rawContent = rssClient.fetchRaw(url)
        if (rawContent.isNullOrBlank()) {
            return RssVerificationResult(false, "Unknown", "Could not fetch URL.")
        }

        // Truncate to avoid huge prompts, usually header info is at the start
        val snippet = rawContent.take(2000)

        val prompt = """
            Analyze the following text snippet from a URL.
            Determine if it appears to be a valid RSS or Atom feed containing news, market analysis, or financial content.
            Return a JSON object with:
            - isValid: boolean
            - title: string (title of the feed, or "Unknown")
            - description: string (short description of the feed, or reason why invalid)
            
            Snippet:
            $snippet
        """.trimIndent()

        val request = LlmStageRequest(
            systemPrompt = "You are a technical validator.",
            userPrompt = prompt,
            stage = AnalysisStage.RSS_VERIFY,
            expectedSchemaId = "RssVerificationResult"
        )

        return try {
            val response = stageModelRouter.run(AnalysisStage.RSS_VERIFY, request)
            val json = extractJson(response.rawText) ?: return RssVerificationResult(false, "Unknown", "Failed to parse validation.")
            
            val obj = JSONObject(json)
            RssVerificationResult(
                isValid = obj.optBoolean("isValid", false),
                title = obj.optString("title", "Unknown"),
                description = obj.optString("description", "No description")
            )
        } catch (e: Exception) {
            Logger.e("VerifyRssFeedUseCase", "Verification failed", e)
            RssVerificationResult(false, "Error", "Verification process failed: ${e.message}")
        }
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
