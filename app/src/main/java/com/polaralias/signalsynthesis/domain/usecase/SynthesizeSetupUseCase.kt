package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.ai.*
import com.polaralias.signalsynthesis.domain.model.AiSynthesis
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.util.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class SynthesizeSetupUseCase(
    private val repository: MarketDataRepository,
    private val stageModelRouter: StageModelRouter
) {
    suspend fun execute(
        setup: TradeSetup,
        llmKey: String,
        reasoningDepth: ReasoningDepth = ReasoningDepth.MEDIUM,
        outputLength: OutputLength = OutputLength.STANDARD,
        verbosity: Verbosity = Verbosity.MEDIUM,
        onProgress: ((String) -> Unit)? = null
    ): AiSynthesis {
        // Step 1: Data Analysis (interpret context + technicals)
        onProgress?.invoke("Data Interpretation...")
        val analysisPrompt = buildAnalysisPrompt(setup, verbosity)
        Logger.i("LLM", "Step 1: Data Analysis for ${setup.symbol}")
        
        val startTime1 = System.currentTimeMillis()
        val analysisReport = try {
            val request = LlmStageRequest(
                systemPrompt = AiPrompts.SYSTEM_ANALYST,
                userPrompt = analysisPrompt,
                stage = AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS,
                maxOutputTokens = when (outputLength) {
                    OutputLength.SHORT -> 400
                    OutputLength.STANDARD -> 800
                    OutputLength.FULL -> 1500
                },
                reasoningDepth = reasoningDepth
            )
            val response = stageModelRouter.run(AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS, request)
            val duration = System.currentTimeMillis() - startTime1
            com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("DataAnalysis", analysisPrompt, response.rawText, true, duration)
            response.rawText
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime1
            com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("DataAnalysis", analysisPrompt, e.message ?: "Failed", false, duration)
            throw e
        }

        // Step 2: Trading Verdict (final plan)
        val verdictPrompt = buildVerdictPrompt(setup, analysisReport, verbosity)
        Logger.i("LLM", "Step 2: Trading Verdict for ${setup.symbol}")
        onProgress?.invoke("Formulating Verdict...")
        
        val startTime2 = System.currentTimeMillis()
        val rawVerdictResponse = try {
            val request = LlmStageRequest(
                systemPrompt = AiPrompts.SYSTEM_ANALYST,
                userPrompt = verdictPrompt,
                stage = AnalysisStage.DECISION_UPDATE,
                expectedSchemaId = "AiSynthesis",
                maxOutputTokens = when (outputLength) {
                    OutputLength.SHORT -> 400
                    OutputLength.STANDARD -> 800
                    OutputLength.FULL -> 1500
                },
                reasoningDepth = reasoningDepth
            )
            val response = stageModelRouter.run(AnalysisStage.DECISION_UPDATE, request)
            val duration = System.currentTimeMillis() - startTime2
            com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("TradingVerdict", verdictPrompt, response.rawText, true, duration)
            response.rawText
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime2
            com.polaralias.signalsynthesis.util.ActivityLogger.logLlm("TradingVerdict", verdictPrompt, e.message ?: "Failed", false, duration)
            throw e
        }

        return parseResponse(rawVerdictResponse)
    }

    private fun buildAnalysisPrompt(setup: TradeSetup, verbosity: Verbosity): String {
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

        val technicalLines = buildTechnicalLines(setup)
        val reasons = if (setup.reasons.isEmpty()) "None" else setup.reasons.joinToString("; ")
        val verbosityDirective = buildVerbosityDirective(verbosity)

        return AiPrompts.STEP_1_DATA_ANALYSIS
            .replace("{symbol}", setup.symbol)
            .replace("{technicalIndicators}", technicalLines.ifEmpty { listOf("N/A") }.joinToString("\n"))
            .replace("{context}", contextLines.ifEmpty { listOf("N/A") }.joinToString("\n"))
            .replace("{reasons}", reasons)
            .trimIndent() + "\n\nVerbosity directive: $verbosityDirective"
    }

    private fun buildVerdictPrompt(setup: TradeSetup, analysisReport: String, verbosity: Verbosity): String {
        val technicalLines = buildTechnicalLines(setup)
        val verbosityDirective = buildVerbosityDirective(verbosity)
        return AiPrompts.STEP_2_TRADING_VERDICT
            .replace("{symbol}", setup.symbol)
            .replace("{intent}", setup.intent.name)
            .replace("{setupType}", setup.setupType)
            .replace("{triggerPrice}", setup.triggerPrice.toString())
            .replace("{stopLoss}", setup.stopLoss.toString())
            .replace("{targetPrice}", setup.targetPrice.toString())
            .replace("{technicalIndicators}", technicalLines.ifEmpty { listOf("N/A") }.joinToString("\n"))
            .replace("{analysisReport}", analysisReport)
            .trimIndent() + "\n\nVerbosity directive: $verbosityDirective"
    }

    private fun buildTechnicalLines(setup: TradeSetup): List<String> {
        val technicalLines = mutableListOf<String>()
        setup.intradayStats?.let {
            it.rsi14?.let { rsi -> technicalLines.add("RSI (14): ${String.format(Locale.US, "%.2f", rsi)}") }
            it.vwap?.let { vwap -> technicalLines.add("VWAP: ${String.format(Locale.US, "%.2f", vwap)}") }
            it.atr14?.let { atr -> technicalLines.add("ATR (14): ${String.format(Locale.US, "%.2f", atr)}") }
        }
        setup.eodStats?.let {
            it.sma50?.let { sma -> technicalLines.add("SMA (50): ${String.format(Locale.US, "%.2f", sma)}") }
            it.sma200?.let { sma -> technicalLines.add("SMA (200): ${String.format(Locale.US, "%.2f", sma)}") }
        }
        return technicalLines
    }

    private fun buildVerbosityDirective(verbosity: Verbosity): String {
        return when (verbosity) {
            Verbosity.LOW -> "Be concise (2-3 sentences per section)."
            Verbosity.MEDIUM -> "Use balanced detail (4-6 sentences per section)."
            Verbosity.HIGH -> "Be detailed (6-10 sentences per section)."
        }
    }

    private fun parseResponse(raw: String): AiSynthesis {
        val trimmed = raw.trim()
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
            } catch (e: Exception) {
                Logger.e("SynthesizeSetupUseCase", "Failed to parse AI synthesis JSON", e)
            }
        }

        return AiSynthesis(summary = trimmed, risks = emptyList(), verdict = "Unavailable")
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
}
