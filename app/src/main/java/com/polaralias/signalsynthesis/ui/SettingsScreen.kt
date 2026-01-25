package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.polaralias.signalsynthesis.data.settings.AppSettings
import com.polaralias.signalsynthesis.domain.ai.*
import com.polaralias.signalsynthesis.ui.theme.*
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

@Composable
private fun AuthStatusItem(label: String, status: String, isActive: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
            Text(status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = if (isActive) com.polaralias.signalsynthesis.ui.theme.NeonGreen else com.polaralias.signalsynthesis.ui.theme.NeonRed, letterSpacing = 1.sp)
        }
        if (isActive) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = com.polaralias.signalsynthesis.ui.theme.NeonGreen, modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.Default.Block, contentDescription = "Inactive", tint = com.polaralias.signalsynthesis.ui.theme.NeonRed, modifier = Modifier.size(20.dp))
        }
    }
}

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
    onRemoveFromBlocklist: (String) -> Unit,
    onArchiveUsage: () -> Unit = {}
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

    AmbientBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        RainbowMcpText(
                            text = "SYSTEM CONFIGURATION", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(16.dp)
        ) {
            SectionHeader("NODE AUTHENTICATION")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val providerStatus = if (uiState.hasAnyApiKeys) "ACTIVE" else "MISSING"
                    val llmStatus = if (uiState.hasLlmKey) "ACTIVE" else "MISSING"
                    
                    AuthStatusItem("MARKET DATA PROTOCOL", providerStatus, uiState.hasAnyApiKeys)
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthStatusItem("AI SYNTHESIS PROTOCOL", llmStatus, uiState.hasLlmKey)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.polaralias.signalsynthesis.ui.theme.NeonBlue.copy(alpha = 0.1f))
                                .border(1.dp, com.polaralias.signalsynthesis.ui.theme.NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { onEditKeys() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("EDIT KEYS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = com.polaralias.signalsynthesis.ui.theme.NeonBlue)
                        }
                        
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { onClearKeys() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("WIPE ALL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("DAILY TELEMETRY USAGE")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TOTAL TRANSACTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(uiState.dailyApiUsage.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = com.polaralias.signalsynthesis.ui.theme.NeonGreen)
                    }
                }
            }
            
            if (uiState.dailyProviderUsage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                uiState.dailyProviderUsage.forEach { (provider, categories) ->
                    val totalForProvider = categories.values.sum()
                    com.polaralias.signalsynthesis.ui.components.GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = provider,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$totalForProvider",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            categories.forEach { (category, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatCategoryName(category).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            if (uiState.archivedUsage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                var showArchive by remember { mutableStateOf(false) }
                
                TextButton(onClick = { showArchive = !showArchive }) {
                    Text(if (showArchive) "Hide Previous Days" else "View Previous Days (${uiState.archivedUsage.size})")
                }
                
                if (showArchive) {
                    uiState.archivedUsage.take(7).forEach { archive ->
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${archive.date}: ${archive.totalCalls} calls",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                archive.providerBreakdown.forEach { (provider, categories) ->
                                    val total = categories.values.sum()
                                    Text(
                                        text = "  $provider: $total",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEditKeys) {
                    Text("Edit Keys")
                }
                OutlinedButton(onClick = onClearKeys) {
                    Text("Clear Keys")
                }
            }
            
            if (uiState.dailyApiUsage > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onArchiveUsage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Archive Today & Reset Counter")
                }
                Text(
                    "Archives today's usage to history and resets the counter. Use this to track usage across multiple analysis sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            SectionHeader("CORE AI INTELLIGENCE")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
            var providerExpanded by remember { mutableStateOf(false) }
            var analysisModelExpanded by remember { mutableStateOf(false) }
            var verdictModelExpanded by remember { mutableStateOf(false) }
            var reasoningModelExpanded by remember { mutableStateOf(false) }

            Text("Global Provider", style = MaterialTheme.typography.labelMedium)
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
                            text = { Text(if(provider == LlmProvider.OPENAI) "OpenAI" else "Gemini") },
                            onClick = {
                                val defaultModel = LlmModel.values().first { it.provider == provider }
                                onUpdateSettings(uiState.appSettings.copy(
                                    llmProvider = provider,
                                    analysisModel = defaultModel,
                                    verdictModel = defaultModel,
                                    reasoningModel = LlmModel.values().firstOrNull { it.provider == provider && it.name.contains("REASONING") } ?: defaultModel
                                ))
                                providerExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Analysis Model (Step 1: Data Interpretation)", style = MaterialTheme.typography.labelMedium)
            ExposedDropdownMenuBox(
                expanded = analysisModelExpanded,
                onExpandedChange = { analysisModelExpanded = !analysisModelExpanded }
            ) {
                TextField(
                    value = formatModelName(uiState.appSettings.analysisModel),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = analysisModelExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = analysisModelExpanded,
                    onDismissRequest = { analysisModelExpanded = false }
                ) {
                    val filteredModels = LlmModel.values().filter { it.provider == uiState.appSettings.llmProvider }
                    for (model in filteredModels) {
                        DropdownMenuItem(
                            text = { Text(formatModelName(model)) },
                            onClick = {
                                onUpdateSettings(uiState.appSettings.copy(analysisModel = model))
                                analysisModelExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Verdict Model (Step 2: Final Thesis)", style = MaterialTheme.typography.labelMedium)
            ExposedDropdownMenuBox(
                expanded = verdictModelExpanded,
                onExpandedChange = { verdictModelExpanded = !verdictModelExpanded }
            ) {
                TextField(
                    value = formatModelName(uiState.appSettings.verdictModel),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = verdictModelExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = verdictModelExpanded,
                    onDismissRequest = { verdictModelExpanded = false }
                ) {
                    val filteredModels = LlmModel.values().filter { it.provider == uiState.appSettings.llmProvider }
                    for (model in filteredModels) {
                        DropdownMenuItem(
                            text = { Text(formatModelName(model)) },
                            onClick = {
                                onUpdateSettings(uiState.appSettings.copy(verdictModel = model))
                                verdictModelExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Reasoning Model (Deep analysis)", style = MaterialTheme.typography.labelMedium)
            ExposedDropdownMenuBox(
                expanded = reasoningModelExpanded,
                onExpandedChange = { reasoningModelExpanded = !reasoningModelExpanded }
            ) {
                TextField(
                    value = formatModelName(uiState.appSettings.reasoningModel),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasoningModelExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = reasoningModelExpanded,
                    onDismissRequest = { reasoningModelExpanded = false }
                ) {
                    val filteredModels = LlmModel.values().filter { it.provider == uiState.appSettings.llmProvider }
                    for (model in filteredModels) {
                        DropdownMenuItem(
                            text = { Text(formatModelName(model)) },
                            onClick = {
                                onUpdateSettings(uiState.appSettings.copy(reasoningModel = model))
                                reasoningModelExpanded = false
                            }
                        )
                }
            }
        }
    }
}


            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("QUANTITATIVE TUNING")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
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
                            val currentModel = uiState.appSettings.analysisModel
                            for (depth in com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.values()) {
                                // Disable EXTRA unless GPT-5.2 or GPT-5.2 Pro
                                val isExtraEnabled = currentModel == LlmModel.GPT_5_2 || currentModel == LlmModel.GPT_5_2_PRO
                                if (depth == com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.EXTRA && !isExtraEnabled) continue

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
                    Spacer(modifier = Modifier.height(16.dp))

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
                    Spacer(modifier = Modifier.height(16.dp))

                    // Verbosity (OpenAI Only)
                    val isGeminiVal = uiState.appSettings.llmProvider == LlmProvider.GEMINI
                    Text(
                        text = if (isGeminiVal) "Verbosity (OpenAI Only)" else "Verbosity",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isGeminiVal) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                    var verbosityExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = verbosityExpanded && !isGeminiVal,
                        onExpandedChange = { if (!isGeminiVal) verbosityExpanded = !verbosityExpanded }
                    ) {
                        TextField(
                            value = if (isGeminiVal) "Prompt-guided only" else uiState.appSettings.verbosity.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isGeminiVal,
                            trailingIcon = { if (!isGeminiVal) ExposedDropdownMenuDefaults.TrailingIcon(expanded = verbosityExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            supportingText = { if (isGeminiVal) Text("Not supported for Gemini models") }
                        )
                        if (!isGeminiVal) {
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
                    Spacer(modifier = Modifier.height(16.dp))

                    // Risk Tolerance
                    Text("Risk Tolerance Profile", style = MaterialTheme.typography.labelMedium)
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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
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
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("BACKGROUND INTELLIGENCE")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE MONITORING",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
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
                        text = if (uiState.alertsEnabled) "SCANNING ${uiState.alertSymbolCount} NODES" else "MONITORING SUSPENDED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.alertsEnabled) com.polaralias.signalsynthesis.ui.theme.NeonBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontWeight = FontWeight.ExtraBold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSlider(
                        label = "POLLING INTERVAL",
                        value = uiState.appSettings.alertCheckIntervalMinutes.toDouble(),
                        range = 1.0f..60.0f,
                        steps = 59,
                        format = { "${it.toInt()} MIN" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(alertCheckIntervalMinutes = it.toInt())) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("TECHNICAL TRIGGER THRESHOLDS")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SettingsSlider(
                        label = "VWAP DIP PROTOCOL",
                        value = uiState.appSettings.vwapDipPercent,
                        range = 0.1f..5.0f,
                        steps = 49,
                        format = { String.format("%.1f%%", it) },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(vwapDipPercent = it)) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSlider(
                        label = "RSI OVERSOLD LIMIT",
                        value = uiState.appSettings.rsiOversold,
                        range = 10.0f..50.0f,
                        steps = 40,
                        format = { it.roundToInt().toString() },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(rsiOversold = it)) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSlider(
                        label = "RSI OVERBOUGHT LIMIT",
                        value = uiState.appSettings.rsiOverbought,
                        range = 50.0f..90.0f,
                        steps = 40,
                        format = { it.roundToInt().toString() },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(rsiOverbought = it)) }
                    )
                    
                    if (uiState.aiThresholdSuggestion != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        AiSuggestionCard(
                            title = "AI THRESHOLD OPTIMIZATION",
                            suggestionText = "VWAP: ${String.format("%.1f%%", uiState.aiThresholdSuggestion.vwapDipPercent)} | RSI: ${uiState.aiThresholdSuggestion.rsiOversold.roundToInt()}/${uiState.aiThresholdSuggestion.rsiOverbought.roundToInt()}",
                            rationale = uiState.aiThresholdSuggestion.rationale,
                            onApply = onApplyAi,
                            onDismiss = onDismissAi
                        )
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.polaralias.signalsynthesis.ui.theme.NeonPurple.copy(alpha = 0.1f))
                                .border(1.dp, com.polaralias.signalsynthesis.ui.theme.NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable(enabled = uiState.hasLlmKey && !uiState.isSuggestingThresholds) { showThresholdAiDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (uiState.isSuggestingThresholds) "SYNTHESIZING..." else "OPTIMIZE WITH AI", 
                                style = MaterialTheme.typography.labelSmall, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = com.polaralias.signalsynthesis.ui.theme.NeonPurple
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("STOCK SCREENER TOLERANCE")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SettingsSlider(
                        label = "CONSERVATIVE LIMIT",
                        value = uiState.appSettings.screenerConservativeThreshold,
                        range = 1.0f..100.0f,
                        steps = 99,
                        format = { "$${it.roundToInt()}" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerConservativeThreshold = it)) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSlider(
                        label = "MODERATE LIMIT",
                        value = uiState.appSettings.screenerModerateThreshold,
                        range = 1.0f..500.0f,
                        steps = 499,
                        format = { "$${it.roundToInt()}" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerModerateThreshold = it)) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSlider(
                        label = "AGGRESSIVE LIMIT",
                        value = uiState.appSettings.screenerAggressiveThreshold,
                        range = 1.0f..2000.0f,
                        steps = 1999,
                        format = { "$${it.roundToInt()}" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerAggressiveThreshold = it)) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSlider(
                        label = "MINIMUM VOLUME",
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
                        Spacer(modifier = Modifier.height(24.dp))
                        AiSuggestionCard(
                            title = "AI SCREENER OPTIMIZATION",
                            suggestionText = "CONS: $${uiState.aiScreenerSuggestion.conservativeLimit.roundToInt()} | MOD: $${uiState.aiScreenerSuggestion.moderateLimit.roundToInt()} | AGGR: $${uiState.aiScreenerSuggestion.aggressiveLimit.roundToInt()} | VOL: ${String.format("%.1fM", uiState.aiScreenerSuggestion.minVolume / 1_000_000.0)}",
                            rationale = uiState.aiScreenerSuggestion.rationale,
                            onApply = onApplyScreenerAi,
                            onDismiss = onDismissScreenerAi
                        )
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.polaralias.signalsynthesis.ui.theme.NeonGreen.copy(alpha = 0.1f))
                                .border(1.dp, com.polaralias.signalsynthesis.ui.theme.NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable(enabled = uiState.hasLlmKey && !uiState.isSuggestingScreener) { showScreenerAiDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (uiState.isSuggestingScreener) "OPTIMIZING..." else "OPTIMIZE SCREENER WITH AI", 
                                style = MaterialTheme.typography.labelSmall, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = com.polaralias.signalsynthesis.ui.theme.NeonGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("SYSTEM NODES")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("ACTIVITY LOGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
                            Text("HISTORICAL TELEMETRY", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onOpenLogs) {
                            Text("ACCESS LOGS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("CUSTOM TICKERS")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    TextField(
                        value = newTicker,
                        onValueChange = { 
                            newTicker = it.uppercase()
                            onSearchTickers(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("SEARCH OR ENTER SYMBOL") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        trailingIcon = {
                            if (newTicker.isNotBlank()) {
                                IconButton(onClick = { 
                                    onAddCustomTicker(newTicker)
                                    newTicker = ""
                                    onClearTickerSearch()
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                            }
                        }
                    )
                    
                    if (uiState.tickerSearchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(uiState.tickerSearchResults) { result ->
                                androidx.compose.material3.ListItem(
                                    headlineContent = { Text(result.symbol, fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text(result.name, fontSize = 10.sp) },
                                    modifier = Modifier.clickable {
                                        onAddCustomTicker(result.symbol)
                                        newTicker = ""
                                        onClearTickerSearch()
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                    
                    if (uiState.customTickers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        uiState.customTickers.forEach { ticker ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(ticker.symbol, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    SourceBadge(ticker.source)
                                }
                                IconButton(onClick = { onRemoveCustomTicker(ticker.symbol) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("BLACK HOLE (BLOCKLIST)")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (uiState.blocklist.isEmpty()) {
                        Text("NO NODES SEGREGATED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    } else {
                        uiState.blocklist.forEach { symbol ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(symbol, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { onRemoveFromBlocklist(symbol) }) {
                                    Text("RESTORE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
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
        else -> model.name
    }
}

private fun formatCategoryName(category: com.polaralias.signalsynthesis.util.ApiUsageCategory): String {
    return when (category) {
        com.polaralias.signalsynthesis.util.ApiUsageCategory.DISCOVERY -> "Finding Tickers"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.ANALYSIS -> "Reviewing Trends"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.FUNDAMENTALS -> "Company Data"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.ALERTS -> "Alert Checks"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.SEARCH -> "Ticker Search"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.OTHER -> "Other"
    }
}
