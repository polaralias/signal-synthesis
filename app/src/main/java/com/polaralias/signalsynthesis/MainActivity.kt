package com.polaralias.signalsynthesis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.work.WorkManager
import androidx.room.Room
import com.polaralias.signalsynthesis.data.db.AppDatabase
import com.polaralias.signalsynthesis.data.provider.ProviderFactory
import com.polaralias.signalsynthesis.data.repository.DatabaseRepository
import com.polaralias.signalsynthesis.data.repository.AiSummaryRepository
import com.polaralias.signalsynthesis.data.repository.RoomDatabaseRepository
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStore
import com.polaralias.signalsynthesis.data.storage.AppSettingsStore
import com.polaralias.signalsynthesis.data.storage.ApiKeyStore
import com.polaralias.signalsynthesis.data.worker.WorkManagerScheduler
import com.polaralias.signalsynthesis.ui.AnalysisViewModel
import com.polaralias.signalsynthesis.ui.SignalSynthesisApp

import com.polaralias.signalsynthesis.util.CrashReporter
import com.polaralias.signalsynthesis.data.settings.ThemeMode

class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "signal-synthesis-db").build()
    }
    private val viewModel: AnalysisViewModel by viewModels {
        AnalysisViewModelFactory(this, db)
    }
    private val notificationSymbol = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.polaralias.signalsynthesis.data.provider.ProviderStatusManager.initialize(this)
        com.polaralias.signalsynthesis.util.UsageTracker.init(this)
        com.polaralias.signalsynthesis.data.provider.ProviderStatusManager.onBlacklisted = { providerName ->
            com.polaralias.signalsynthesis.util.NotificationHelper.showBlacklistNotification(this, providerName)
        }
        CrashReporter.init(true)
        notificationSymbol.value = intent?.getStringExtra(EXTRA_SYMBOL)
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.lifecycle.compose.LocalLifecycleOwner provides this
            ) {
                val symbol by notificationSymbol
                AppContent(viewModel = viewModel, initialSymbol = symbol)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        notificationSymbol.value = intent.getStringExtra(EXTRA_SYMBOL)
    }

    companion object {
        const val EXTRA_SYMBOL = "extra_symbol"
    }
}

@Composable
private fun AppContent(viewModel: AnalysisViewModel, initialSymbol: String?) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useDarkTheme = when (uiState.appSettings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    com.polaralias.signalsynthesis.ui.theme.SignalSynthesisTheme(darkTheme = useDarkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SignalSynthesisApp(viewModel, initialSymbol)
        }
    }
}

private class AnalysisViewModelFactory(
    private val activity: ComponentActivity,
    private val db: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            val providerFactory = ProviderFactory()
            val keyStore = ApiKeyStore(activity)
            val alertStore = AlertSettingsStore(activity)
            val workManager = WorkManager.getInstance(activity)
            val workScheduler = WorkManagerScheduler(workManager)
            val llmClientFactory = com.polaralias.signalsynthesis.data.ai.LlmClientFactory()
            val dbRepository = RoomDatabaseRepository(db.watchlistDao(), db.historyDao())
            val appSettingsStore = AppSettingsStore(activity)
            val aiSummaryRepository = AiSummaryRepository(db.aiSummaryDao())
            @Suppress("UNCHECKED_CAST")
            return AnalysisViewModel(
                providerFactory = providerFactory,
                keyStore = keyStore,
                alertStore = alertStore,
                workScheduler = workScheduler,
                llmClientFactory = llmClientFactory,
                dbRepository = dbRepository,
                appSettingsStore = appSettingsStore,
                aiSummaryRepository = aiSummaryRepository,
                application = activity.application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
