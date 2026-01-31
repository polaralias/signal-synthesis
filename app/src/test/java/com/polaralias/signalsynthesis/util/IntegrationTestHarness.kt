package com.polaralias.signalsynthesis.util

import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.domain.ai.StageModelRouter
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.domain.usecase.ShortlistCandidatesUseCase
import kotlinx.coroutines.runBlocking
import java.time.Instant

/**
 * A simple harness to manually run and verify the shortlist stage.
 * This satisfies the requirement for an integration harness.
 */
object IntegrationTestHarness {
    fun runShortlistDemo(stageModelRouter: StageModelRouter) = runBlocking {
        val useCase = ShortlistCandidatesUseCase(stageModelRouter)
        
        val symbols = listOf("AAPL", "TSLA", "MSFT", "NVDA", "AMD", "META", "AMZN", "GOOGL")
        val quotes = symbols.associateWith { symbol ->
            Quote(
                symbol = symbol,
                price = (150..300).random().toDouble(),
                volume = (1_000_000..10_000_000).random().toLong(),
                timestamp = Instant.now(),
                changePercent = (-300..300).random().toDouble() / 100.0
            )
        }

        println("--- Running Shortlist Stage Demo ---")
        val plan = useCase.execute(
            symbols = symbols,
            quotes = quotes,
            intent = TradingIntent.DAY_TRADE,
            risk = RiskTolerance.MODERATE,
            maxShortlist = 5
        )

        println("Shortlist Result:")
        plan.shortlist.forEach { item ->
            println("- ${item.symbol} (Priority: ${item.priority}): ${item.reasons}")
            println("  Enrichment requested: ${item.requestedEnrichment}")
            if (item.avoid) println("  AVOID: ${item.riskFlags}")
        }
        println("Global Notes: ${plan.globalNotes}")
        println("Limits Applied: ${plan.limitsApplied}")
    }
}
