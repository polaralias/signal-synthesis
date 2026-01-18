package com.polaralias.signalsynthesis.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun SignalSynthesisApp(viewModel: AnalysisViewModel, initialSymbol: String? = null) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycleOwner = lifecycleOwner)
    val lastHandledSymbol = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialSymbol) {
        val symbol = initialSymbol?.trim().orEmpty()
        if (symbol.isNotEmpty() && symbol != lastHandledSymbol.value) {
            navController.navigate(Screen.detailRoute(symbol))
            lastHandledSymbol.value = symbol
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
                }
            )
        }
        composable(Screen.Analysis.route) {
            AnalysisScreen(
                uiState = uiState,
                onIntentSelected = viewModel::updateIntent,
                onRunAnalysis = viewModel::runAnalysis,
                onOpenKeys = { navController.navigate(Screen.Keys.route) },
                onOpenResults = { navController.navigate(Screen.Results.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenWatchlist = { navController.navigate(Screen.Watchlist.route) },
                onOpenHistory = { navController.navigate(Screen.History.route) },
                onDismissError = viewModel::clearError
            )
        }
        composable(Screen.Watchlist.route) {
            WatchlistScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onOpenSymbol = { symbol ->
                    navController.navigate(Screen.detailRoute(symbol))
                },
                onRemove = viewModel::toggleWatchlist
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
                onToggleWatchlist = viewModel::toggleWatchlist
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
                onDismissAi = viewModel::dismissAiSuggestion
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Analysis : Screen("analysis")
    object Keys : Screen("keys")
    object Results : Screen("results")
    object Settings : Screen("settings")
    object Watchlist : Screen("watchlist")
    object History : Screen("history")
    object Dashboard : Screen("dashboard")

    object Detail : Screen("detail/{symbol}") {
        const val ARG_SYMBOL = "symbol"
    }

    companion object {
        fun detailRoute(symbol: String): String {
            return "detail/${Uri.encode(symbol)}"
        }
    }
}
