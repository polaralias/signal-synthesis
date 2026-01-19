package com.polaralias.signalsynthesis.data.settings

import com.polaralias.signalsynthesis.domain.ai.LlmModel
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.ReasoningDepth
import com.polaralias.signalsynthesis.domain.ai.OutputLength
import com.polaralias.signalsynthesis.domain.ai.Verbosity

data class AppSettings(
    val quoteRefreshIntervalMinutes: Int = 5,
    val alertCheckIntervalMinutes: Int = 15,
    val vwapDipPercent: Double = 1.0,
    val rsiOversold: Double = 30.0,
    val rsiOverbought: Double = 70.0,
    val useMockDataWhenOffline: Boolean = true,
    val llmProvider: LlmProvider = LlmProvider.OPENAI,
    val llmModel: LlmModel = LlmModel.GPT_5_1,
    val reasoningDepth: ReasoningDepth = ReasoningDepth.BALANCED,
    val outputLength: OutputLength = OutputLength.STANDARD,
    val verbosity: Verbosity = Verbosity.MEDIUM,
    val riskTolerance: RiskTolerance = RiskTolerance.MODERATE
)

enum class RiskTolerance { CONSERVATIVE, MODERATE, AGGRESSIVE }
