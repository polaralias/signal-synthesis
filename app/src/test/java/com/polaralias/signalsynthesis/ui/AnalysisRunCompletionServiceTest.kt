package com.polaralias.signalsynthesis.ui

import com.polaralias.signalsynthesis.data.alerts.AlertDirection
import com.polaralias.signalsynthesis.data.alerts.AlertTarget
import com.polaralias.signalsynthesis.data.alerts.AlertSettings
import com.polaralias.signalsynthesis.data.alerts.AlertType
import com.polaralias.signalsynthesis.data.repository.DatabaseRepository
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStorage
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AnalysisRunCompletionServiceTest {

    @Test
    fun handleCompletedAnalysisPersistsHistoryAlertsAndNotifications() = runTest {
        val repository = RecordingDatabaseRepository()
        val alertStore = RecordingAlertSettingsStore()
        val notifier = RecordingTradeSignalNotifier()
        val service = AnalysisRunCompletionService(repository, alertStore, notifier)
        val result = AnalysisResult(
            intent = TradingIntent.SWING,
            totalCandidates = 2,
            tradeableCount = 2,
            setupCount = 2,
            setups = listOf(
                sampleSetup("AAPL", triggerPrice = 100.0, targetPrice = 110.0, confidence = 0.92),
                sampleSetup("TSLA", triggerPrice = 210.0, targetPrice = 205.0, confidence = 0.71)
            ),
            generatedAt = Instant.parse("2026-05-24T12:00:00Z")
        )

        val outcome = service.handleCompletedAnalysis(
            result = result,
            removedAlerts = setOf("TSLA"),
            blocklist = listOf("MSFT")
        )

        assertEquals(result, repository.savedHistory.single())
        assertEquals(listOf("AAPL"), alertStore.savedSymbols)
        assertEquals(
            listOf(AlertTarget(symbol = "AAPL", targetPrice = 110.0, direction = AlertDirection.ABOVE)),
            alertStore.savedTargets
        )
        assertEquals(listOf("AAPL"), notifier.notifiedSymbols)
        assertEquals(1, outcome.alertSymbolCount)
        assertEquals(listOf("AAPL"), outcome.alertSymbols)
    }

    @Test
    fun handleCompletedAnalysisUsesBelowDirectionForBearishTargets() = runTest {
        val result = AnalysisResult(
            intent = TradingIntent.DAY_TRADE,
            totalCandidates = 1,
            tradeableCount = 1,
            setupCount = 1,
            setups = listOf(sampleSetup("NVDA", triggerPrice = 120.0, targetPrice = 115.0, confidence = 0.4)),
            generatedAt = Instant.parse("2026-05-24T12:00:00Z")
        )
        val alertStore = RecordingAlertSettingsStore()
        val verificationService = AnalysisRunCompletionService(
            RecordingDatabaseRepository(),
            alertStore,
            RecordingTradeSignalNotifier()
        )

        verificationService.handleCompletedAnalysis(
            result = result,
            removedAlerts = emptySet(),
            blocklist = emptyList()
        )

        assertEquals(AlertDirection.BELOW, alertStore.savedTargets.single().direction)
    }

    private fun sampleSetup(
        symbol: String,
        triggerPrice: Double,
        targetPrice: Double,
        confidence: Double
    ): TradeSetup {
        return TradeSetup(
            symbol = symbol,
            setupType = "Momentum",
            triggerPrice = triggerPrice,
            stopLoss = triggerPrice - 2.0,
            targetPrice = targetPrice,
            confidence = confidence,
            reasons = listOf("sample"),
            validUntil = Instant.parse("2026-05-25T12:00:00Z"),
            intent = TradingIntent.SWING
        )
    }

    private class RecordingDatabaseRepository : DatabaseRepository {
        val savedHistory = mutableListOf<AnalysisResult>()

        override suspend fun addToWatchlist(symbol: String, intent: TradingIntent?) = Unit
        override suspend fun removeFromWatchlist(symbol: String) = Unit
        override fun getWatchlist(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun saveHistory(result: AnalysisResult) {
            savedHistory += result
        }
        override fun getHistory(): Flow<List<AnalysisResult>> = flowOf(emptyList())
        override suspend fun clearHistory() = Unit
    }

    private class RecordingAlertSettingsStore : AlertSettingsStorage {
        var savedSymbols: List<String> = emptyList()
        var savedTargets: List<AlertTarget> = emptyList()

        override suspend fun loadSettings(): AlertSettings = AlertSettings()
        override suspend fun saveSettings(settings: AlertSettings) = Unit
        override suspend fun loadSymbols(): List<String> = savedSymbols
        override suspend fun saveSymbols(symbols: List<String>) {
            savedSymbols = symbols
        }
        override suspend fun loadTargets(): List<AlertTarget> = savedTargets
        override suspend fun saveTargets(targets: List<AlertTarget>) {
            savedTargets = targets
        }
        override suspend fun getLastAlertTimestamp(symbol: String, type: AlertType): Long = 0L
        override suspend fun setLastAlertTimestamp(symbol: String, type: AlertType, timestamp: Long) = Unit
    }

    private class RecordingTradeSignalNotifier : TradeSignalNotifier {
        val notifiedSymbols = mutableListOf<String>()

        override fun notify(setup: TradeSetup) {
            notifiedSymbols += setup.symbol
        }
    }
}
