package com.polaralias.signalsynthesis.ui

import android.app.Application
import com.polaralias.signalsynthesis.data.alerts.AlertDirection
import com.polaralias.signalsynthesis.data.alerts.AlertTarget
import com.polaralias.signalsynthesis.data.repository.DatabaseRepository
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStorage
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.util.NotificationHelper

data class AnalysisCompletionOutcome(
    val alertSymbolCount: Int,
    val alertSymbols: List<String>
)

interface TradeSignalNotifier {
    fun notify(setup: TradeSetup)
}

class SystemTradeSignalNotifier(
    private val application: Application
) : TradeSignalNotifier {
    override fun notify(setup: TradeSetup) {
        NotificationHelper.showTradeSignal(
            context = application,
            symbol = setup.symbol,
            setupType = setup.setupType,
            confidence = setup.confidence,
            intent = setup.intent
        )
    }
}

class AnalysisRunCompletionService(
    private val dbRepository: DatabaseRepository,
    private val alertStore: AlertSettingsStorage,
    private val notifier: TradeSignalNotifier
) {
    suspend fun handleCompletedAnalysis(
        result: AnalysisResult,
        removedAlerts: Set<String>,
        blocklist: List<String>
    ): AnalysisCompletionOutcome {
        dbRepository.saveHistory(result)

        val resultsWithoutRemoved = result.setups.filter { !removedAlerts.contains(it.symbol) }
        val symbols = resultsWithoutRemoved
            .map { it.symbol }
            .distinct()
            .filter { !blocklist.contains(it) }
        alertStore.saveSymbols(symbols)

        val targets = resultsWithoutRemoved
            .filter { symbols.contains(it.symbol) }
            .map { setup ->
                val direction = if (setup.targetPrice >= setup.triggerPrice) {
                    AlertDirection.ABOVE
                } else {
                    AlertDirection.BELOW
                }
                AlertTarget(
                    symbol = setup.symbol,
                    targetPrice = setup.targetPrice,
                    direction = direction
                )
            }
        alertStore.saveTargets(targets)

        result.setups
            .filter { it.confidence > 0.8 }
            .forEach(notifier::notify)

        return AnalysisCompletionOutcome(
            alertSymbolCount = symbols.size,
            alertSymbols = symbols
        )
    }
}
