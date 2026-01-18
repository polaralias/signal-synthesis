package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.model.AiSynthesis
import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.SentimentData
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import org.json.JSONArray
import org.json.JSONObject

class SynthesizeSetupUseCase(
    private val repository: MarketDataRepository,
    private val llmClient: LlmClient
) {
    suspend fun execute(setup: TradeSetup, llmKey: String): AiSynthesis {
        val profile = safeFetch { repository.getProfile(setup.symbol) }
        val metrics = safeFetch { repository.getMetrics(setup.symbol) }
        val sentiment = safeFetch { repository.getSentiment(setup.symbol) }

        val prompt = buildPrompt(setup, profile, metrics, sentiment)
        val rawResponse = llmClient.generate(prompt, apiKey = llmKey)
        return parseResponse(rawResponse)
    }

    private fun buildPrompt(
        setup: TradeSetup,
        profile: CompanyProfile?,
        metrics: FinancialMetrics?,
        sentiment: SentimentData?
    ): String {
        val contextLines = mutableListOf<String>()
        profile?.let {
            contextLines.add("Company: ${it.name}")
            contextLines.add("Sector: ${it.sector ?: "Unknown"}")
            contextLines.add("Industry: ${it.industry ?: "Unknown"}")
            contextLines.add("Description: ${it.description ?: "N/A"}")
        }
        metrics?.let {
            contextLines.add("Market cap: ${it.marketCap ?: "N/A"}")
            contextLines.add("PE ratio: ${it.peRatio ?: "N/A"}")
            contextLines.add("EPS: ${it.eps ?: "N/A"}")
        }
        sentiment?.let {
            contextLines.add("Sentiment score: ${it.score ?: "N/A"}")
            contextLines.add("Sentiment label: ${it.label ?: "N/A"}")
        }

        val reasons = if (setup.reasons.isEmpty()) "None" else setup.reasons.joinToString("; ")

        return """
            Act as a senior trading analyst. Review the following setup and return JSON only.
            Output schema:
            {
              "summary": "string",
              "risks": ["string"],
              "verdict": "string"
            }

            Ticker: ${setup.symbol}
            Setup: ${setup.setupType}
            Intent: ${setup.intent}
            Trigger: ${setup.triggerPrice}
            Stop: ${setup.stopLoss}
            Target: ${setup.targetPrice}
            Confidence: ${setup.confidence}
            Reasons: $reasons
            Context:
            ${contextLines.joinToString("\n")}
        """.trimIndent()
    }

    private fun parseResponse(raw: String): AiSynthesis {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return AiSynthesis(
                summary = "No AI response received.",
                risks = emptyList(),
                verdict = "Unavailable"
            )
        }

        val json = extractJson(trimmed)
        if (json != null) {
            try {
                val obj = JSONObject(json)
                val risks = obj.optJSONArray("risks").toStringList()
                val summary = obj.optString("summary", trimmed)
                val verdict = obj.optString("verdict", "Unavailable")
                return AiSynthesis(
                    summary = summary.ifBlank { trimmed },
                    risks = risks,
                    verdict = verdict.ifBlank { "Unavailable" }
                )
            } catch (_: Exception) {
                // Fallback to plain text below.
            }
        }

        return AiSynthesis(
            summary = trimmed,
            risks = emptyList(),
            verdict = "Unavailable"
        )
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val items = mutableListOf<String>()
        for (index in 0 until length()) {
            items.add(optString(index))
        }
        return items.filter { it.isNotBlank() }
    }

    private suspend fun <T> safeFetch(block: suspend () -> T?): T? {
        return try {
            block()
        } catch (_: Exception) {
            null
        }
    }
}
