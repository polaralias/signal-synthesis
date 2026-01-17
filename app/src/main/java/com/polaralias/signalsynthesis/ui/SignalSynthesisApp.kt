package com.polaralias.signalsynthesis.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    NavHost(
        navController = navController,
        startDestination = Screen.Analysis.route
    ) {
        composable(Screen.Analysis.route) {
            AnalysisScreen(
                uiState = uiState,
                onIntentSelected = viewModel::updateIntent,
                onRunAnalysis = viewModel::runAnalysis,
                onOpenKeys = { navController.navigate(Screen.Keys.route) },
                onOpenResults = { navController.navigate(Screen.Results.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onDismissError = viewModel::clearError
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
                }
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
                onRequestSummary = viewModel::requestAiSummary
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onEditKeys = { navController.navigate(Screen.Keys.route) },
                onClearKeys = viewModel::clearKeys,
                onToggleAlerts = viewModel::updateAlertsEnabled
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Analysis : Screen("analysis")
    object Keys : Screen("keys")
    object Results : Screen("results")
    object Settings : Screen("settings")

    object Detail : Screen("detail/{symbol}") {
        const val ARG_SYMBOL = "symbol"
    }

    companion object {
        fun detailRoute(symbol: String): String {
            return "detail/${Uri.encode(symbol)}"
        }
    }
}
