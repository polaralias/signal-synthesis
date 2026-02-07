package com.polaralias.signalsynthesis.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polaralias.signalsynthesis.data.alerts.AlertSettings
import com.polaralias.signalsynthesis.data.alerts.AlertTarget
import com.polaralias.signalsynthesis.data.alerts.AlertType
import com.polaralias.signalsynthesis.data.provider.ApiKeys
import com.polaralias.signalsynthesis.data.provider.MarketDataProviderFactory
import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.repository.AiSummaryRepository
import com.polaralias.signalsynthesis.data.repository.DatabaseRepository
import com.polaralias.signalsynthesis.data.settings.AppSettings
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStorage
import com.polaralias.signalsynthesis.data.storage.ApiKeyStorage
import com.polaralias.signalsynthesis.data.storage.AppSettingsStorage
import com.polaralias.signalsynthesis.data.storage.LlmKeys
import com.polaralias.signalsynthesis.data.worker.WorkScheduler
import com.polaralias.signalsynthesis.ui.theme.SignalSynthesisTheme
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun intentChipsUpdateSelection() {
        var state by mutableStateOf(AnalysisUiState(intent = TradingIntent.DAY_TRADE))

        composeRule.setContent {
            SignalSynthesisTheme {
                AnalysisScreen(
                    uiState = state,
                    onBack = {},
                    onIntentSelected = { intent -> state = state.copy(intent = intent) },
                    onAssetClassSelected = {},
                    onDiscoveryModeSelected = {},
                    onRunAnalysis = {},
                    onOpenKeys = {},
                    onOpenResults = {},
                    onOpenSettings = {},
                    onOpenWatchlist = {},
                    onOpenHistory = {},
                    onDismissError = {},
                    onClearNavigation = {},
                    onCancelAnalysis = {},
                    onTogglePause = {}
                )
            }
        }

        composeRule.onNodeWithText("SWING").performClick()
        composeRule.runOnIdle {
            assertEquals(TradingIntent.SWING, state.intent)
        }
    }

    @Test
    fun errorMessageDisplaysInAnalysisScreen() {
        val errorState = AnalysisUiState(errorMessage = "Synthetic error")

        composeRule.setContent {
            SignalSynthesisTheme {
                AnalysisScreen(
                    uiState = errorState,
                    onBack = {},
                    onIntentSelected = {},
                    onAssetClassSelected = {},
                    onDiscoveryModeSelected = {},
                    onRunAnalysis = {},
                    onOpenKeys = {},
                    onOpenResults = {},
                    onOpenSettings = {},
                    onOpenWatchlist = {},
                    onOpenHistory = {},
                    onDismissError = {},
                    onClearNavigation = {},
                    onCancelAnalysis = {},
                    onTogglePause = {}
                )
            }
        }

        composeRule.onNodeWithText("SYSTEM EXCEPTION").assertIsDisplayed()
        composeRule.onNodeWithText("Synthetic error").assertIsDisplayed()
    }

    @Test
    fun dashboardNavigatesToAnalysisScreen() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = createViewModel(application)

        composeRule.setContent {
            SignalSynthesisTheme {
                SignalSynthesisApp(viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("DAY TRADE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("DISCOVERY PROTOCOL").assertIsDisplayed()
    }

    private fun createViewModel(application: Application): AnalysisViewModel {
        return AnalysisViewModel(
            providerFactory = FakeProviderFactory(),
            keyStore = FakeApiKeyStore(),
            alertStore = FakeAlertSettingsStore(),
            workScheduler = FakeWorkScheduler(),
            dbRepository = FakeDatabaseRepository(),
            appSettingsStore = FakeAppSettingsStore(),
            aiSummaryRepository = AiSummaryRepository(FakeAiSummaryDao()),
            rssDao = FakeRssDao(),
            application = application,
            ioDispatcher = Dispatchers.Main
        )
    }

    private class FakeProviderFactory : MarketDataProviderFactory {
        override fun build(keys: ApiKeys): ProviderBundle = ProviderBundle.empty()
    }

    private class FakeApiKeyStore : ApiKeyStorage {
        override suspend fun loadApiKeys(): ApiKeys = ApiKeys()
        override suspend fun loadLlmKeys(): LlmKeys = LlmKeys()
        override suspend fun saveKeys(apiKeys: ApiKeys, llmKeys: LlmKeys) {}
        override suspend fun clear() {}
    }

    private class FakeAlertSettingsStore : AlertSettingsStorage {
        override suspend fun loadSettings(): AlertSettings = AlertSettings()
        override suspend fun saveSettings(settings: AlertSettings) {}
        override suspend fun loadSymbols(): List<String> = emptyList()
        override suspend fun saveSymbols(symbols: List<String>) {}
        override suspend fun loadTargets(): List<AlertTarget> = emptyList()
        override suspend fun saveTargets(targets: List<AlertTarget>) {}
        override suspend fun getLastAlertTimestamp(symbol: String, type: AlertType): Long = 0L
        override suspend fun setLastAlertTimestamp(symbol: String, type: AlertType, timestamp: Long) {}
    }

    private class FakeWorkScheduler : WorkScheduler {
        override fun scheduleAlerts(enabled: Boolean, intervalMinutes: Int) {}
    }

    private class FakeDatabaseRepository : DatabaseRepository {
        override suspend fun addToWatchlist(symbol: String, intent: TradingIntent?) {}
        override suspend fun removeFromWatchlist(symbol: String) {}
        override fun getWatchlist(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun saveHistory(result: AnalysisResult) {}
        override fun getHistory(): Flow<List<AnalysisResult>> = flowOf(emptyList())
        override suspend fun clearHistory() {}
    }

    private class FakeAppSettingsStore : AppSettingsStorage {
        override suspend fun loadSettings(): AppSettings = AppSettings()
        override suspend fun saveSettings(settings: AppSettings) {}
    }

    private class FakeAiSummaryDao : com.polaralias.signalsynthesis.data.db.dao.AiSummaryDao {
        override suspend fun getByKey(symbol: String, model: String, promptHash: String): com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity? = null
        override fun getAll(): Flow<List<com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity>> = flowOf(emptyList())
        override suspend fun insert(summary: com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity) {}
        override suspend fun delete(summary: com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity) {}
        override suspend fun clear() {}
    }

    private class FakeRssDao : com.polaralias.signalsynthesis.data.rss.RssDao {
        override suspend fun getFeedState(url: String): com.polaralias.signalsynthesis.data.rss.RssFeedStateEntity? = null
        override suspend fun insertFeedState(state: com.polaralias.signalsynthesis.data.rss.RssFeedStateEntity) {}
        override suspend fun getAllRecentItems(since: Long): List<com.polaralias.signalsynthesis.data.rss.RssItemEntity> = emptyList()
        override suspend fun getItemsForFeed(url: String, limit: Int): List<com.polaralias.signalsynthesis.data.rss.RssItemEntity> = emptyList()
        override suspend fun insertItems(items: List<com.polaralias.signalsynthesis.data.rss.RssItemEntity>) {}
        override suspend fun deleteOldItems(threshold: Long) {}
    }
}
