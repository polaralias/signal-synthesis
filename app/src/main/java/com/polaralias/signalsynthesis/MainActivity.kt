package com.polaralias.signalsynthesis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.polaralias.signalsynthesis.data.ai.OpenAiLlmClient
import com.polaralias.signalsynthesis.data.ai.OpenAiService
import com.polaralias.signalsynthesis.data.provider.ProviderFactory
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStore
import com.polaralias.signalsynthesis.data.storage.ApiKeyStore
import com.polaralias.signalsynthesis.data.worker.WorkManagerScheduler
import com.polaralias.signalsynthesis.ui.AnalysisViewModel
import com.polaralias.signalsynthesis.ui.SignalSynthesisApp

class MainActivity : ComponentActivity() {
    private val viewModel: AnalysisViewModel by viewModels {
        AnalysisViewModelFactory(this)
    }
    private val notificationSymbol = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationSymbol.value = intent?.getStringExtra(EXTRA_SYMBOL)
        setContent {
            val symbol by notificationSymbol
            AppContent(viewModel = viewModel, initialSymbol = symbol)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        notificationSymbol.value = intent?.getStringExtra(EXTRA_SYMBOL)
    }

    companion object {
        const val EXTRA_SYMBOL = "extra_symbol"
    }
}

@Composable
private fun AppContent(viewModel: AnalysisViewModel, initialSymbol: String?) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SignalSynthesisApp(viewModel, initialSymbol)
        }
    }
}

private class AnalysisViewModelFactory(
    private val activity: ComponentActivity
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            val providerFactory = ProviderFactory()
            val keyStore = ApiKeyStore(activity)
            val alertStore = AlertSettingsStore(activity)
            val workManager = WorkManager.getInstance(activity)
            val workScheduler = WorkManagerScheduler(workManager)
            val llmService = OpenAiService.create()
            val llmClient = OpenAiLlmClient(llmService)
            @Suppress("UNCHECKED_CAST")
            return AnalysisViewModel(
                providerFactory = providerFactory,
                keyStore = keyStore,
                alertStore = alertStore,
                workScheduler = workScheduler,
                llmClient = llmClient
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
