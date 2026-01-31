package com.polaralias.signalsynthesis.ui

import com.polaralias.signalsynthesis.data.alerts.AlertSettings
import com.polaralias.signalsynthesis.data.provider.ApiKeys
import com.polaralias.signalsynthesis.data.provider.MarketDataProviderFactory
import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStorage
import com.polaralias.signalsynthesis.data.storage.ApiKeyStorage
import com.polaralias.signalsynthesis.data.worker.WorkScheduler
import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.model.AiSynthesis
import com.polaralias.signalsynthesis.data.repository.DatabaseRepository
import com.polaralias.signalsynthesis.data.repository.AiSummaryRepository
import com.polaralias.signalsynthesis.data.settings.AppSettings
import com.polaralias.signalsynthesis.data.storage.AppSettingsStorage
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsCorrect() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        // Allow init to complete
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        // hasAnyApiKeys depends on FakeApiKeyStore default which is false
        assertEquals(false, state.hasAnyApiKeys)
    }

    @Test
    fun updateKeyUpdatesState() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.updateKey(KeyField.ALPACA_KEY, "test_key")

        val state = viewModel.uiState.value
        assertEquals("test_key", state.keys.alpacaKey)
    }

    @Test
    fun runAnalysisFailsWithoutKeys() = runTest(testDispatcher) {
        val viewModel = createViewModel(hasKeys = false)
        testDispatcher.scheduler.advanceUntilIdle() // let init finish

        viewModel.runAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Add at least one provider key"))
    }

    @Test
    fun runAnalysisSucceedsWithKeys() = runTest(testDispatcher) {
        val viewModel = createViewModel(hasKeys = true)
        testDispatcher.scheduler.advanceUntilIdle() // let init finish

        viewModel.runAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.errorMessage)
        // Result might be empty because fake provider returns nothing, but it shouldn't error
        assertNotNull(state.result)
    }

    @Test
    fun addCustomTickerUpdatesState() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.addCustomTicker("AAPL")

        val state = viewModel.uiState.value
        assertTrue(state.customTickers.any { it.symbol == "AAPL" })
    }

    private fun createViewModel(
        hasKeys: Boolean = false
    ): AnalysisViewModel {
        val keyStore = FakeApiKeyStore(hasKeys)
        return AnalysisViewModel(
            providerFactory = FakeProviderFactory(),
            keyStore = keyStore,
            alertStore = FakeAlertSettingsStore(),
            workScheduler = FakeWorkScheduler(),
            llmClientFactory = FakeLlmClientFactory(),
            dbRepository = FakeDatabaseRepository(),
            appSettingsStore = FakeAppSettingsStore(),
            aiSummaryRepository = AiSummaryRepository(FakeAiSummaryDao()),
            rssDao = FakeRssDao(),
            application = FakeApplication(),
            clock = clock,
            ioDispatcher = testDispatcher
        )
    }

    private class FakeProviderFactory : MarketDataProviderFactory {
        override fun build(keys: ApiKeys): ProviderBundle {
            return ProviderBundle(
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
            )
        }
    }

    private class FakeApiKeyStore(private val initialHasKeys: Boolean) : ApiKeyStorage {
        override suspend fun loadApiKeys(): ApiKeys {
            return if (initialHasKeys) ApiKeys(alpacaKey = "test", alpacaSecret = "test") else ApiKeys()
        }
        override suspend fun loadLlmKey(): String? = null
        override suspend fun saveKeys(apiKeys: ApiKeys, llmKey: String?) {}
        override suspend fun clear() {}
    }

    private class FakeAlertSettingsStore : AlertSettingsStorage {
        override suspend fun loadSettings(): AlertSettings = AlertSettings()
        override suspend fun saveSettings(settings: AlertSettings) {}
        override suspend fun loadSymbols(): List<String> = emptyList()
        override suspend fun saveSymbols(symbols: List<String>) {}
    }

    private class FakeWorkScheduler : WorkScheduler {
        override fun scheduleAlerts(enabled: Boolean, intervalMinutes: Int) {}
    }

    private class FakeDatabaseRepository : DatabaseRepository {
        override suspend fun addToWatchlist(symbol: String) {}
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
        override suspend fun getBySymbol(symbol: String): com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity? = null
        override fun getAll(): Flow<List<com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity>> = flowOf(emptyList())
        override suspend fun insert(summary: com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity) {}
        override suspend fun delete(summary: com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity) {}
        override suspend fun clear() {}
    }

    private class FakeRssDao : com.polaralias.signalsynthesis.data.rss.RssDao {
        override suspend fun getFeedState(url: String): com.polaralias.signalsynthesis.data.rss.RssFeedStateEntity? = null
        override suspend fun insertFeedState(state: com.polaralias.signalsynthesis.data.rss.RssFeedStateEntity) {}
        override suspend fun getAllRecentItems(since: Long): List<com.polaralias.signalsynthesis.data.rss.RssItemEntity> = emptyList()
        override suspend fun insertItems(items: List<com.polaralias.signalsynthesis.data.rss.RssItemEntity>) {}
        override suspend fun deleteOldItems(threshold: Long) {}
    }

    private class FakeLlmClientFactory : com.polaralias.signalsynthesis.data.ai.LlmClientFactory() {
        override fun create(model: com.polaralias.signalsynthesis.domain.ai.LlmModel): LlmClient {
            return FakeLlmClient()
        }
    }

    private class FakeLlmClient : LlmClient {
        override suspend fun generate(
            prompt: String,
            systemPrompt: String?,
            apiKey: String,
            reasoningDepth: com.polaralias.signalsynthesis.domain.ai.ReasoningDepth,
            outputLength: com.polaralias.signalsynthesis.domain.ai.OutputLength,
            verbosity: com.polaralias.signalsynthesis.domain.ai.Verbosity
        ): String {
            return """
                {
                  "summary": "Fake summary",
                  "risks": ["Fake risk"],
                  "verdict": "Fake verdict"
                }
            """.trimIndent()
        }
    }

    private class FakeApplication : android.app.Application()
}
