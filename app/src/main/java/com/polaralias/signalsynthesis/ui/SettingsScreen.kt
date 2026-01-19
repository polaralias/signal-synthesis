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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onEditKeys: () -> Unit,
    onClearKeys: () -> Unit,
    onToggleAlerts: (Boolean) -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onSuggestAi: (String) -> Unit,
    onApplyAi: () -> Unit,
    onDismissAi: () -> Unit,
    onOpenLogs: () -> Unit
) {
    var showAiDialog by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleAlerts(true)
            com.polaralias.signalsynthesis.util.Logger.i("Settings", "Notification permission granted")
        } else {
            com.polaralias.signalsynthesis.util.Logger.w("Settings", "Notification permission denied")
        }
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
                .padding(16.dp)
        ) {
            SectionHeader("Key Status")
            val providerStatus = if (uiState.hasAnyApiKeys) "Configured" else "Missing"
            val llmStatus = if (uiState.hasLlmKey) "Configured" else "Missing"
            Text("Provider keys: $providerStatus")
            Text("LLM key: $llmStatus")

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

            SectionHeader("Alert Thresholds")
            
            SettingsSlider(
                label = "VWAP Dip %",
                value = uiState.appSettings.vwapDipPercent,
                range = 0.1f..5.0f,
                steps = 49,
                format = { String.format("%.1f%%", it) },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(vwapDipPercent = it)) }
            )

            SettingsSlider(
                label = "RSI Oversold",
                value = uiState.appSettings.rsiOversold,
                range = 10.0f..50.0f,
                steps = 40,
                format = { it.roundToInt().toString() },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(rsiOversold = it)) }
            )

            SettingsSlider(
                label = "RSI Overbought",
                value = uiState.appSettings.rsiOverbought,
                range = 50.0f..90.0f,
                steps = 40,
                format = { it.roundToInt().toString() },
                onValueChange = { onUpdateSettings(uiState.appSettings.copy(rsiOverbought = it)) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            if (uiState.aiThresholdSuggestion != null) {
                AiSuggestionCard(
                    suggestion = uiState.aiThresholdSuggestion,
                    onApply = onApplyAi,
                    onDismiss = onDismissAi
                )
            } else {
                Button(
                    onClick = { showAiDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.hasLlmKey && !uiState.isSuggestingThresholds
                ) {
                    Text(if (uiState.isSuggestingThresholds) "Analyzing..." else "Ask AI for suggestions")
                }
            }

            SectionHeader("App")
            Text(
                text = "AI synthesis runs when an LLM key is configured. Alerts run every 15 minutes when enabled.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenLogs,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View System Logs")
            }
        }
    }

    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("Ask AI for Thresholds") },
            text = {
                Column {
                    Text("Describe your trading style or risk tolerance (e.g., 'I am a conservative swing trader' or 'I have $5000 to invest').")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter context...") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSuggestAi(aiPrompt)
                    showAiDialog = false
                }) {
                    Text("Suggest")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAiDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
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
    suggestion: com.polaralias.signalsynthesis.ui.AiThresholdSuggestion,
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
                text = "AI Suggestions",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "VWAP Dip: ${String.format("%.1f%%", suggestion.vwapDipPercent)}")
            Text(text = "RSI Oversold: ${suggestion.rsiOversold.roundToInt()}")
            Text(text = "RSI Overbought: ${suggestion.rsiOverbought.roundToInt()}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = suggestion.rationale,
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
