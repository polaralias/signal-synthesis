package com.polaralias.signalsynthesis.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun SignalSynthesisApp(viewModel: AnalysisViewModel, initialSymbol: String? = null) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastHandledSymbol = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialSymbol) {
        val symbol = initialSymbol?.trim().orEmpty()
        if (symbol.isNotEmpty() && symbol != lastHandledSymbol.value) {
            navController.navigate(Screen.detailRoute(symbol))
            lastHandledSymbol.value = symbol
        }
    }

    LaunchedEffect(uiState.navigationEvent) {
        when (uiState.navigationEvent) {
            NavigationEvent.Results -> {
                navController.navigate(Screen.Results.route)
                viewModel.clearNavigation()
            }
            NavigationEvent.Alerts -> {
                navController.navigate(Screen.Alerts.route)
                viewModel.clearNavigation()
            }
            null -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                uiState = uiState,
                onIntentSelected = { intent ->
                    viewModel.updateIntent(intent)
                    navController.navigate(Screen.Analysis.route)
                },
                onRefreshMarket = viewModel::refreshMarketOverview,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenResults = { navController.navigate(Screen.Results.route) },
                onOpenDetail = { symbol ->
                    navController.navigate(Screen.detailRoute(symbol))
                },
                onOpenAlertsList = { navController.navigate(Screen.Alerts.route) },
                onRemoveTicker = viewModel::removeAlert,
                onBlockTicker = viewModel::addToBlocklist
            )
        }
        composable(Screen.Analysis.route) {
            AnalysisScreen(
                uiState = uiState,
                onIntentSelected = viewModel::updateIntent,
                onAssetClassSelected = viewModel::updateAssetClass,
                onDiscoveryModeSelected = viewModel::updateDiscoveryMode,
                onRunAnalysis = viewModel::runAnalysis,
                onOpenKeys = { navController.navigate(Screen.Keys.route) },
                onOpenResults = { navController.navigate(Screen.Results.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenWatchlist = { navController.navigate(Screen.Watchlist.route) },
                onOpenHistory = { navController.navigate(Screen.History.route) },
                onDismissError = viewModel::clearError,
                onClearNavigation = viewModel::clearNavigation,
                onCancelAnalysis = viewModel::cancelAnalysis,
                onTogglePause = viewModel::togglePause
            )
        }
        composable(Screen.Watchlist.route) {
            WatchlistScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onOpenSymbol = { symbol ->
                    navController.navigate(Screen.detailRoute(symbol))
                },
                onRemove = viewModel::toggleWatchlist,
                onBlock = viewModel::addToBlocklist
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onClearHistory = viewModel::clearHistory,
                onViewResult = { result ->
                    viewModel.showHistoricalResult(result)
                    navController.navigate(Screen.Results.route)
                }
            )
        }
        composable(Screen.Keys.route) {
            ApiKeysScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onFieldChanged = viewModel::updateKey,
                onSave = viewModel::saveKeys
            )
        }
        composable(Screen.Results.route) {
            ResultsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onOpenDetail = { symbol ->
                    navController.navigate(Screen.detailRoute(symbol))
                },
                onToggleWatchlist = viewModel::toggleWatchlist,
                onRemoveTicker = viewModel::removeAlert,
                onBlockTicker = viewModel::addToBlocklist
            )
        }
        composable(Screen.Alerts.route) {
            MarketAlertsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onOpenDetail = { symbol ->
                    navController.navigate(Screen.detailRoute(symbol))
                },
                onRemoveAlert = viewModel::removeAlert,
                onAddToBlocklist = viewModel::addToBlocklist
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument(Screen.Detail.ARG_SYMBOL) { type = NavType.StringType })
        ) { entry ->
            val symbol = entry.arguments?.getString(Screen.Detail.ARG_SYMBOL).orEmpty()
            SetupDetailScreen(
                uiState = uiState,
                symbol = Uri.decode(symbol),
                onBack = { navController.popBackStack() },
                onRequestSummary = viewModel::requestAiSummary,
                onRequestChartData = viewModel::requestChartData,
                onToggleWatchlist = viewModel::toggleWatchlist
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onEditKeys = { navController.navigate(Screen.Keys.route) },
                onClearKeys = viewModel::clearKeys,
                onToggleAlerts = viewModel::updateAlertsEnabled,
                onUpdateSettings = viewModel::updateAppSettings,
                onSuggestAi = viewModel::suggestThresholdsWithAi,
                onApplyAi = viewModel::applyAiThresholdSuggestion,
                onDismissAi = viewModel::dismissAiSuggestion,
                onOpenLogs = { navController.navigate(Screen.Logs.route) },
                onAddCustomTicker = viewModel::addCustomTicker,
                onRemoveCustomTicker = viewModel::removeCustomTicker,
                onSearchTickers = viewModel::searchTickers,
                onClearTickerSearch = viewModel::clearTickerSearch,
                onSuggestScreenerAi = viewModel::suggestScreenerWithAi,
                onApplyScreenerAi = viewModel::applyAiScreenerSuggestion,
                onDismissScreenerAi = viewModel::dismissAiScreenerSuggestion,
                onRemoveFromBlocklist = viewModel::removeFromBlocklist
            )
        }
        composable(Screen.Logs.route) {
            LogViewerScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Analysis : Screen("analysis")
    object Keys : Screen("keys")
    object Results : Screen("results")
    object Alerts : Screen("alerts")
    object Settings : Screen("settings")
    object Watchlist : Screen("watchlist")
    object History : Screen("history")
    object Dashboard : Screen("dashboard")
    object Logs : Screen("logs")

    object Detail : Screen("detail/{symbol}") {
        const val ARG_SYMBOL = "symbol"
    }

    companion object {
        fun detailRoute(symbol: String): String {
            return "detail/${Uri.encode(symbol)}"
        }
    }
}
