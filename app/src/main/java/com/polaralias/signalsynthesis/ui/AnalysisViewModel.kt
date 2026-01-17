package com.polaralias.signalsynthesis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.signalsynthesis.data.provider.MarketDataProviderFactory
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStorage
import com.polaralias.signalsynthesis.data.storage.ApiKeyStorage
import com.polaralias.signalsynthesis.data.worker.WorkScheduler
import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.domain.usecase.RunAnalysisUseCase
import com.polaralias.signalsynthesis.domain.usecase.SynthesizeSetupUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

class AnalysisViewModel(
    private val providerFactory: MarketDataProviderFactory,
    private val keyStore: ApiKeyStorage,
    private val alertStore: AlertSettingsStorage,
    private val workScheduler: WorkScheduler,
    private val llmClient: LlmClient,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        refreshKeys()
        refreshAlerts()
    }

    fun updateIntent(intent: TradingIntent) {
        _uiState.update { it.copy(intent = intent) }
    }

    fun updateKey(field: KeyField, value: String) {
        _uiState.update { state ->
            val keys = when (field) {
                KeyField.ALPACA_KEY -> state.keys.copy(alpacaKey = value)
                KeyField.ALPACA_SECRET -> state.keys.copy(alpacaSecret = value)
                KeyField.POLYGON -> state.keys.copy(polygonKey = value)
                KeyField.FINNHUB -> state.keys.copy(finnhubKey = value)
                KeyField.FMP -> state.keys.copy(fmpKey = value)
                KeyField.LLM -> state.keys.copy(llmKey = value)
            }
            state.copy(keys = keys)
        }
    }

    fun saveKeys() {
        viewModelScope.launch(ioDispatcher) {
            val keys = _uiState.value.keys
            keyStore.saveKeys(keys.toApiKeys(), keys.llmKey)
            refreshKeys()
        }
    }

    fun clearKeys() {
        viewModelScope.launch(ioDispatcher) {
            keyStore.clear()
            refreshKeys()
        }
    }

    fun runAnalysis() {
        val apiKeys = _uiState.value.keys.toApiKeys()
        if (!apiKeys.hasAny()) {
            _uiState.update { it.copy(errorMessage = "Add at least one provider key before running analysis.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, aiSummaries = emptyMap()) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val useCase = buildUseCase(apiKeys)
                val result = useCase.execute(_uiState.value.intent)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = result,
                        lastRunAt = result.generatedAt
                    )
                }
                val symbols = result.setups.map { it.symbol }.distinct()
                alertStore.saveSymbols(symbols)
                _uiState.update { it.copy(alertSymbolCount = symbols.size) }
            } catch (ex: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ex.message ?: "Analysis failed. Please try again."
                    )
                }
            }
        }
    }

    fun requestAiSummary(symbol: String) {
        val state = _uiState.value
        val llmKey = state.keys.llmKey
        if (llmKey.isBlank() || !state.hasLlmKey) return
        val setup = state.result?.setups?.firstOrNull { it.symbol == symbol } ?: return
        val existing = state.aiSummaries[symbol]
        if (existing?.status == AiSummaryStatus.LOADING || existing?.status == AiSummaryStatus.READY) return

        updateAiSummary(symbol, AiSummaryState(status = AiSummaryStatus.LOADING))
        viewModelScope.launch(ioDispatcher) {
            try {
                val useCase = buildSynthesisUseCase(state.keys.toApiKeys())
                val synthesis = useCase.execute(setup, llmKey)
                updateAiSummary(
                    symbol,
                    AiSummaryState(
                        status = AiSummaryStatus.READY,
                        summary = synthesis.summary,
                        risks = synthesis.risks,
                        verdict = synthesis.verdict
                    )
                )
            } catch (ex: Exception) {
                updateAiSummary(
                    symbol,
                    AiSummaryState(
                        status = AiSummaryStatus.ERROR,
                        errorMessage = ex.message ?: "AI synthesis failed."
                    )
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun updateAlertsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(alertsEnabled = enabled) }
        viewModelScope.launch(ioDispatcher) {
            val current = alertStore.loadSettings()
            alertStore.saveSettings(current.copy(enabled = enabled))
            workScheduler.scheduleAlerts(enabled)
        }
    }

    private fun refreshKeys() {
        viewModelScope.launch(ioDispatcher) {
            val apiKeys = keyStore.loadApiKeys()
            val llmKey = keyStore.loadLlmKey()
            _uiState.update {
                it.copy(
                    keys = ApiKeyUiState.from(apiKeys, llmKey),
                    hasAnyApiKeys = apiKeys.hasAny(),
                    hasLlmKey = !llmKey.isNullOrBlank()
                )
            }
        }
    }

    private fun refreshAlerts() {
        viewModelScope.launch(ioDispatcher) {
            val settings = alertStore.loadSettings()
            val symbols = alertStore.loadSymbols()
            _uiState.update {
                it.copy(
                    alertsEnabled = settings.enabled,
                    alertSymbolCount = symbols.size
                )
            }
            workScheduler.scheduleAlerts(settings.enabled)
        }
    }

    private fun buildUseCase(apiKeys: com.polaralias.signalsynthesis.data.provider.ApiKeys): RunAnalysisUseCase {
        val bundle = providerFactory.build(apiKeys)
        val repository = MarketDataRepository(bundle)
        return RunAnalysisUseCase(repository, clock)
    }

    private fun buildSynthesisUseCase(
        apiKeys: com.polaralias.signalsynthesis.data.provider.ApiKeys
    ): SynthesizeSetupUseCase {
        val bundle = providerFactory.build(apiKeys)
        val repository = MarketDataRepository(bundle)
        return SynthesizeSetupUseCase(repository, llmClient)
    }

    private fun updateAiSummary(symbol: String, summary: AiSummaryState) {
        _uiState.update { state ->
            state.copy(aiSummaries = state.aiSummaries + (symbol to summary))
        }
    }
}

enum class KeyField {
    ALPACA_KEY,
    ALPACA_SECRET,
    POLYGON,
    FINNHUB,
    FMP,
    LLM
}
