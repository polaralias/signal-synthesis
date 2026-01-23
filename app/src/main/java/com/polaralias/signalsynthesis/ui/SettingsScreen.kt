package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.polaralias.signalsynthesis.data.settings.AppSettings
import com.polaralias.signalsynthesis.domain.ai.LlmModel
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onEditKeys: () -> Unit,
    onClearKeys: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onToggleAlerts: (Boolean) -> Unit,
    onSuggestAi: (String) -> Unit,
    onApplyAi: () -> Unit,
    onDismissAi: () -> Unit,
    onOpenLogs: () -> Unit,
    onAddCustomTicker: (String) -> Unit,
    onRemoveCustomTicker: (String) -> Unit,
    onSearchTickers: (String) -> Unit,
    onClearTickerSearch: () -> Unit,
    onSuggestScreenerAi: (String) -> Unit,
    onApplyScreenerAi: () -> Unit,
    onDismissScreenerAi: () -> Unit,
    onRemoveFromBlocklist: (String) -> Unit
) {
    var showThresholdAiDialog by remember { mutableStateOf(false) }
    var showScreenerAiDialog by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }
    var newTicker by remember { mutableStateOf("") }
    val context = LocalContext.current

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleAlerts(true)
            com.polaralias.signalsynthesis.util.Logger.i("Settings", "Notification permission granted")
        } else {
            // Permission denied
            onToggleAlerts(false)
            showPermissionDeniedDialog = true
            com.polaralias.signalsynthesis.util.Logger.w("Settings", "Notification permission denied")
        }
    }
    
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Permission Required") },
            text = { Text("Notifications are required for background alerts. Please enable them in your device settings.") },
            confirmButton = {
                Button(onClick = { showPermissionDeniedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(16.dp)
        ) {
            SectionHeader("Key Status")
            val providerStatus = if (uiState.hasAnyApiKeys) "Configured" else "Missing"
            val llmStatus = if (uiState.hasLlmKey) "Configured" else "Missing"
            Text("Provider keys: $providerStatus")
            Text("LLM key: $llmStatus")
            
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("Usage Status")
            Text("Total Monthly API Requests: ${uiState.monthlyApiUsage}", fontWeight = FontWeight.Bold)
            if (uiState.monthlyProviderUsage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                uiState.monthlyProviderUsage.forEach { (provider, count) ->
                    Text("$provider: $count", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Usage is monitored per provider to help you stay within individual limits.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Button(onClick = onEditKeys) {
                    Text("Edit Keys")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                OutlinedButton(onClick = onClearKeys) {
                    Text("Clear Keys")
                }
            }

            SectionHeader("AI Model")
            var providerExpanded by remember { mutableStateOf(false) }
            var modelExpanded by remember { mutableStateOf(false) }

            Text("Provider", style = MaterialTheme.typography.labelMedium)
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = !providerExpanded }
            ) {
                TextField(
                    value = uiState.appSettings.llmProvider.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    for (provider in LlmProvider.values()) {
                        DropdownMenuItem(
                            text = { Text(provider.name) },
                            onClick = {
                                val defaultModel = LlmModel.values().first { it.provider == provider }
                                onUpdateSettings(uiState.appSettings.copy(
                                    llmProvider = provider,
                                    llmModel = defaultModel
                                ))
                                providerExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Model", style = MaterialTheme.typography.labelMedium)
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = !modelExpanded }
            ) {
                TextField(
                    value = formatModelName(uiState.appSettings.llmModel),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    val filteredModels = LlmModel.values().filter { it.provider == uiState.appSettings.llmProvider }
                    for (model in filteredModels) {
                        DropdownMenuItem(
                            text = { Text(formatModelName(model)) },
                            onClick = {
                                onUpdateSettings(uiState.appSettings.copy(llmModel = model))
                                modelExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Reasoning Depth
            Text("Reasoning Depth", style = MaterialTheme.typography.labelMedium)
            var reasoningExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = reasoningExpanded,
                onExpandedChange = { reasoningExpanded = !reasoningExpanded }
            ) {
                TextField(
                    value = uiState.appSettings.reasoningDepth.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasoningExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = reasoningExpanded,
                    onDismissRequest = { reasoningExpanded = false }
                ) {
                    val currentModel = uiState.appSettings.llmModel
                    for (depth in com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.values()) {
                        // Disable EXTRA unless GPT-5.2 or GPT-5.2 Pro
                        val isExtraEnabled = currentModel == LlmModel.GPT_5_2 || currentModel == LlmModel.GPT_5_2_PRO
                        if (depth == com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.EXTRA && !isExtraEnabled) continue

                        // Gemini 2.5/3 only supports up to HIGH (handled in client mapping, but here we can hide if needed)
                        // Spec says "Show only reasoning levels supported by the selected model"
                        
                        DropdownMenuItem(
                            text = { Text(depth.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onUpdateSettings(uiState.appSettings.copy(reasoningDepth = depth))
                                reasoningExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Output Length
            Text("Output Length", style = MaterialTheme.typography.labelMedium)
            var lengthExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = lengthExpanded,
                onExpandedChange = { lengthExpanded = !lengthExpanded }
            ) {
                TextField(
                    value = uiState.appSettings.outputLength.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lengthExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = lengthExpanded,
                    onDismissRequest = { lengthExpanded = false }
                ) {
                    for (length in com.polaralias.signalsynthesis.domain.ai.OutputLength.values()) {
                        DropdownMenuItem(
                            text = { Text(length.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onUpdateSettings(uiState.appSettings.copy(outputLength = length))
                                lengthExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Verbosity (OpenAI Only)
            val isGemini = uiState.appSettings.llmProvider == LlmProvider.GEMINI
            Text(
                text = if (isGemini) "Verbosity (OpenAI Only)" else "Verbosity",
                style = MaterialTheme.typography.labelMedium,
                color = if (isGemini) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
            )
            var verbosityExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = verbosityExpanded && !isGemini,
                onExpandedChange = { if (!isGemini) verbosityExpanded = !verbosityExpanded }
            ) {
                TextField(
                    value = if (isGemini) "Prompt-guided only" else uiState.appSettings.verbosity.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isGemini,
                    trailingIcon = { if (!isGemini) ExposedDropdownMenuDefaults.TrailingIcon(expanded = verbosityExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    supportingText = { if (isGemini) Text("Not supported for Gemini models") }
                )
                if (!isGemini) {
                    ExposedDropdownMenu(
                        expanded = verbosityExpanded,
                        onDismissRequest = { verbosityExpanded = false }
                    ) {
                        for (verbosity in com.polaralias.signalsynthesis.domain.ai.Verbosity.values()) {
                            DropdownMenuItem(
                                text = { Text(verbosity.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    onUpdateSettings(uiState.appSettings.copy(verbosity = verbosity))
                                    verbosityExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.height(12.dp))

            // Risk Tolerance
            Text("Risk Tolerance", style = MaterialTheme.typography.labelMedium)
            var riskExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = riskExpanded,
                onExpandedChange = { riskExpanded = !riskExpanded }
            ) {
                TextField(
                    value = uiState.appSettings.riskTolerance.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = riskExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    supportingText = { 
                        if (uiState.appSettings.riskTolerance == com.polaralias.signalsynthesis.data.settings.RiskTolerance.AGGRESSIVE) {
                            Text("Aggressive mode may include low-priced, speculative stocks.")
                        }
                    }
                )
                ExposedDropdownMenu(
                    expanded = riskExpanded,
                    onDismissRequest = { riskExpanded = false }
                ) {
                    for (risk in com.polaralias.signalsynthesis.data.settings.RiskTolerance.values()) {
                        DropdownMenuItem(
                            text = { Text(risk.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onUpdateSettings(uiState.appSettings.copy(riskTolerance = risk))
                                riskExpanded = false
                            }
                        )
                    }
                }
            }

            SectionHeader("Alerts")
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Enable background alerts",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = uiState.alertsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    onToggleAlerts(true)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                onToggleAlerts(true)
                            }
                        } else {
                            onToggleAlerts(false)
                        }
                    }
                )
            }
            Text(
                text = "Monitoring ${uiState.alertSymbolCount} symbols from your last analysis run.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSlider(
                label = "Alert Interval (Minutes)",
                value = uiState.appSettings.alertCheckIntervalMinutes.toDouble(),
                range = 1.0f..60.0f,
                steps = 59,
                format = { "${it.toInt()} min" },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(alertCheckIntervalMinutes = it.toInt())) }
            )
            Text(
                text = "Lower intervals check more frequently but consume more API quota.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )

            SectionHeader("Alert Thresholds")
            Text(
                text = "Configure the technical triggers for real-time alerts.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            SettingsSlider(
                label = "VWAP Dip %",
                value = uiState.appSettings.vwapDipPercent,
                range = 0.1f..5.0f,
                steps = 49,
                format = { String.format("%.1f%%", it) },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(vwapDipPercent = it)) }
            )
            Text("Triggers when price drops below the Volume Weighted Average Price by this percentage.", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(8.dp))
            SettingsSlider(
                label = "RSI Oversold",
                value = uiState.appSettings.rsiOversold,
                range = 10.0f..50.0f,
                steps = 40,
                format = { it.roundToInt().toString() },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(rsiOversold = it)) }
            )
            Text("Triggers when the Relative Strength Index falls below this level (indicates potential reversal).", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(8.dp))
            SettingsSlider(
                label = "RSI Overbought",
                value = uiState.appSettings.rsiOverbought,
                range = 50.0f..90.0f,
                steps = 40,
                format = { it.roundToInt().toString() },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(rsiOverbought = it)) }
            )
            Text("Triggers when the RSI rises above this level (indicates potentially overextended).", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(8.dp))
            
            if (uiState.aiThresholdSuggestion != null) {
                AiSuggestionCard(
                    title = "AI Alert Suggestions",
                    suggestionText = "VWAP Dip: ${String.format("%.1f%%", uiState.aiThresholdSuggestion.vwapDipPercent)}\nRSI Oversold: ${uiState.aiThresholdSuggestion.rsiOversold.roundToInt()}\nRSI Overbought: ${uiState.aiThresholdSuggestion.rsiOverbought.roundToInt()}",
                    rationale = uiState.aiThresholdSuggestion.rationale,
                    onApply = onApplyAi,
                    onDismiss = onDismissAi
                )
            } else {
                Button(
                    onClick = { showThresholdAiDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.hasLlmKey && !uiState.isSuggestingThresholds
                ) {
                    Text(if (uiState.isSuggestingThresholds) "Analyzing..." else "Ask AI for alert suggestions")
                }
            }

            SectionHeader("Stock Screener Tolerance")
            Text(
                text = "Tweak the max price thresholds used during automated discovery for each risk profile.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingsSlider(
                label = "Conservative (Max Price)",
                value = uiState.appSettings.screenerConservativeThreshold,
                range = 1.0f..100.0f,
                steps = 99,
                format = { "$${it.roundToInt()}" },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerConservativeThreshold = it)) }
            )

            SettingsSlider(
                label = "Moderate (Max Price)",
                value = uiState.appSettings.screenerModerateThreshold,
                range = 1.0f..500.0f,
                steps = 499,
                format = { "$${it.roundToInt()}" },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerModerateThreshold = it)) }
            )

            SettingsSlider(
                label = "Aggressive (Max Price)",
                value = uiState.appSettings.screenerAggressiveThreshold,
                range = 1.0f..2000.0f,
                steps = 1999,
                format = { "$${it.roundToInt()}" },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerAggressiveThreshold = it)) }
            )

            SettingsSlider(
                label = "Min Volume",
                value = uiState.appSettings.screenerMinVolume.toDouble(),
                range = 100000f..5000000f,
                steps = 49,
                format = { 
                    val vol = it.toLong()
                    if (vol >= 1_000_000) String.format("%.1fM", vol / 1_000_000.0)
                    else String.format("%dK", vol / 1_000)
                },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerMinVolume = it.toLong())) }
            )

            if (uiState.aiScreenerSuggestion != null) {
                AiSuggestionCard(
                    title = "AI Screener Suggestions",
                    suggestionText = "Cons: $${uiState.aiScreenerSuggestion.conservativeLimit.roundToInt()}\n" +
                                     "Mod: $${uiState.aiScreenerSuggestion.moderateLimit.roundToInt()}\n" +
                                     "Aggr: $${uiState.aiScreenerSuggestion.aggressiveLimit.roundToInt()}\n" +
                                     "Min Vol: ${String.format("%.1fM", uiState.aiScreenerSuggestion.minVolume / 1_000_000.0)}",
                    rationale = uiState.aiScreenerSuggestion.rationale,
                    onApply = onApplyScreenerAi,
                    onDismiss = onDismissScreenerAi
                )
            } else {
                Button(
                    onClick = { showScreenerAiDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.hasLlmKey && !uiState.isSuggestingScreener
                ) {
                    Text(if (uiState.isSuggestingScreener) "Analyzing..." else "Ask AI for screener suggestions")
                }
            }

            SectionHeader("App")
            Text(
                text = "AI synthesis runs when an LLM key is configured.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenLogs,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Activity Logs")
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Custom Tickers")
            Text(
                text = "Manually add tickers to include in every analysis run.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = newTicker,
                    onValueChange = { 
                        newTicker = it.uppercase()
                        onSearchTickers(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search or type symbol (e.g. NVDA)") },
                    singleLine = true,
                    trailingIcon = {
                        if (newTicker.isNotBlank()) {
                            IconButton(onClick = { 
                                onAddCustomTicker(newTicker)
                                newTicker = ""
                                onClearTickerSearch()
                            }) {
                                Text("Add")
                            }
                        }
                    }
                )
                
                if (uiState.tickerSearchResults.isNotEmpty()) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        tonalElevation = 8.dp,
                        shadowElevation = 4.dp
                    ) {
                        LazyColumn {
                            items(uiState.tickerSearchResults) { result ->
                                androidx.compose.material3.ListItem(
                                    headlineContent = { Text(result.symbol) },
                                    supportingContent = { Text(result.name) },
                                    overlineContent = { if (result.exchange != null) Text(result.exchange) },
                                    modifier = Modifier.clickable {
                                        onAddCustomTicker(result.symbol)
                                        newTicker = ""
                                        onClearTickerSearch()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column {
                uiState.customTickers.forEach { ticker ->
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(ticker.symbol) },
                        trailingContent = {
                            IconButton(onClick = { onRemoveCustomTicker(ticker.symbol) }) {
                                Text("✕")
                            }
                        },
                        overlineContent = { SourceBadge(ticker.source) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Blocklist")
            Text(
                text = "Tickers on this list are permanently excluded from analysis and alerts.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (uiState.blocklist.isEmpty()) {
                Text("No tickers blocklisted.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            } else {
                Column {
                    for (symbol in uiState.blocklist) {
                        ListItem(
                            headlineContent = { Text(symbol) },
                            trailingContent = {
                                TextButton(onClick = { onRemoveFromBlocklist(symbol) }) {
                                    Text("Reintroduce")
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showThresholdAiDialog) {
        AiPromptDialog(
            title = "Suggest Alert Thresholds",
            aiPrompt = aiPrompt,
            onPromptChange = { aiPrompt = it },
            onDismiss = { showThresholdAiDialog = false },
            onConfirm = {
                onSuggestAi(aiPrompt)
                showThresholdAiDialog = false
            }
        )
    }

    if (showScreenerAiDialog) {
        AiPromptDialog(
            title = "Suggest Screener Thresholds",
            aiPrompt = aiPrompt,
            onPromptChange = { aiPrompt = it },
            onDismiss = { showScreenerAiDialog = false },
            onConfirm = {
                onSuggestScreenerAi(aiPrompt)
                showScreenerAiDialog = false
            }
        )
    }
}

@Composable
private fun AiPromptDialog(
    title: String,
    aiPrompt: String,
    onPromptChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Describe your trading style or risk tolerance (e.g., 'I am a conservative swing trader').")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = aiPrompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter context...") }
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Suggest")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Double,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Double) -> String,
    onValueChange: (Double) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = label, modifier = Modifier.weight(1f))
            Text(text = format(value))
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = range,
            steps = steps
        )
    }
}

@Composable
private fun AiSuggestionCard(
    title: String,
    suggestionText: String,
    rationale: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = suggestionText, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AI Rationale:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Button(onClick = onApply) {
                    Text("Apply")
                }
                Spacer(modifier = Modifier.padding(4.dp))
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

private fun formatModelName(model: LlmModel): String {
    return when (model) {
        LlmModel.GPT_5_2 -> "GPT-5.2"
        LlmModel.GPT_5_1 -> "GPT-5.1"
        LlmModel.GPT_5_MINI -> "GPT-5 Mini"
        LlmModel.GPT_5_NANO -> "GPT-5 Nano"
        LlmModel.GPT_5_2_PRO -> "GPT-5.2 Pro"
        LlmModel.GEMINI_2_5_FLASH -> "Gemini 2.5 Flash"
        LlmModel.GEMINI_2_5_PRO -> "Gemini 2.5 Pro"
        LlmModel.GEMINI_3_FLASH -> "Gemini 3 Flash"
        LlmModel.GEMINI_3_PRO -> "Gemini 3 Pro"
    }
}
