package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.model.AiSynthesis
import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.SentimentData
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.util.Logger
import org.json.JSONArray
import org.json.JSONObject

class SynthesizeSetupUseCase(
    private val repository: MarketDataRepository,
    private val llmClient: LlmClient
) {
    suspend fun execute(
        setup: TradeSetup,
        llmKey: String,
        reasoningDepth: com.polaralias.signalsynthesis.domain.ai.ReasoningDepth = com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.BALANCED,
        outputLength: com.polaralias.signalsynthesis.domain.ai.OutputLength = com.polaralias.signalsynthesis.domain.ai.OutputLength.STANDARD,
        verbosity: com.polaralias.signalsynthesis.domain.ai.Verbosity = com.polaralias.signalsynthesis.domain.ai.Verbosity.MEDIUM
    ): AiSynthesis {
        val prompt = buildPrompt(setup)
        Logger.i("LLM", "Sending synthesis request for ${setup.symbol}")
        Logger.d("LLM", "Prompt: $prompt")
        
        try {
            val rawResponse = llmClient.generate(
                prompt = prompt,
                apiKey = llmKey,
                reasoningDepth = reasoningDepth,
                outputLength = outputLength,
                verbosity = verbosity
            )
            Logger.i("LLM", "Received synthesis response for ${setup.symbol}")
            Logger.d("LLM", "Response: $rawResponse")
            return parseResponse(rawResponse)
        } catch (e: Exception) {
            Logger.e("LLM", "Failed to generate synthesis for ${setup.symbol}", e)
            throw e
        }
    }

    private fun buildPrompt(setup: TradeSetup): String {
        val contextLines = mutableListOf<String>()
        setup.profile?.let {
            contextLines.add("Company: ${it.name}")
            contextLines.add("Sector: ${it.sector ?: "Unknown"}")
            contextLines.add("Industry: ${it.industry ?: "Unknown"}")
            contextLines.add("Description: ${it.description ?: "N/A"}")
        }
        setup.metrics?.let {
            contextLines.add("Market cap: ${it.marketCap ?: "N/A"}")
            contextLines.add("PE ratio: ${it.peRatio ?: "N/A"}")
            contextLines.add("EPS: ${it.eps ?: "N/A"}")
            contextLines.add("Upcoming Earnings: ${it.earningsDate ?: "N/A"}")
            contextLines.add("Dividend Yield: ${it.dividendYield ?: "N/A"}")
            contextLines.add("P/B Ratio: ${it.pbRatio ?: "N/A"}")
            contextLines.add("Debt-to-Equity: ${it.debtToEquity ?: "N/A"}")
        }
        setup.sentiment?.let {
            contextLines.add("Sentiment score: ${it.score ?: "N/A"}")
            contextLines.add("Sentiment label: ${it.label ?: "N/A"}")
        }

        val technicalLines = mutableListOf<String>()
        setup.intradayStats?.let {
            it.rsi14?.let { rsi -> technicalLines.add("RSI (14): ${String.format("%.2f", rsi)}") }
            it.vwap?.let { vwap -> technicalLines.add("VWAP: ${String.format("%.2f", vwap)}") }
            it.atr14?.let { atr -> technicalLines.add("ATR (14): ${String.format("%.2f", atr)}") }
        }
        setup.eodStats?.let {
            it.sma50?.let { sma -> technicalLines.add("SMA (50): ${String.format("%.2f", sma)}") }
            it.sma200?.let { sma -> technicalLines.add("SMA (200): ${String.format("%.2f", sma)}") }
        }

        val reasons = if (setup.reasons.isEmpty()) "None" else setup.reasons.joinToString("; ")

        return com.polaralias.signalsynthesis.domain.ai.AiPrompts.SETUP_SYNTHESIS_TEMPLATE
            .replace("{symbol}", setup.symbol)
            .replace("{setupType}", setup.setupType)
            .replace("{intent}", setup.intent.name)
            .replace("{triggerPrice}", setup.triggerPrice.toString())
            .replace("{stopLoss}", setup.stopLoss.toString())
            .replace("{targetPrice}", setup.targetPrice.toString())
            .replace("{confidence}", setup.confidence.toString())
            .replace("{reasons}", reasons)
            .replace("{technicalIndicators}", technicalLines.joinToString("\n"))
            .replace("{context}", contextLines.joinToString("\n"))
            .trimIndent()
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
