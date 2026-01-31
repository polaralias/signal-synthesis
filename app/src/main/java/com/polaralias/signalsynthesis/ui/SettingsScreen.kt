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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
private fun AuthStatusItem(label: String, status: String, isActive: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = if (isActive) BrandPrimary else ErrorRed, letterSpacing = 1.sp)
        }
        if (isActive) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = BrandPrimary, modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.Default.Cancel, contentDescription = "Inactive", tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
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
    onUpdateStageConfig: (com.polaralias.signalsynthesis.domain.model.AnalysisStage, StageModelConfig) -> Unit = { _, _ -> },
    onArchiveUsage: () -> Unit = {},
    onAddRssFeed: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onRemoveRssFeed: (String) -> Unit = {}
) {
    var showThresholdAiDialog by remember { mutableStateOf(false) }
    var showScreenerAiDialog by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }
    var newTicker by remember { mutableStateOf("") }
    var newRssFeed by remember { mutableStateOf("") }
    var isAddingRss by remember { mutableStateOf(false) }
    var rssAddResult by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
            title = { Text("PROTOCOL ERROR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
            text = { Text("Notifications are required for background telemetry. Please enable them in system settings.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("RETRY", fontWeight = FontWeight.Black)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    AmbientBackground {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandPrimary)
                        }
                        
                        RainbowMcpText(
                            text = "CONFIGURATION",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                        )

                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Transparent)
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader("APPEARANCE")
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("INTERFACE THEME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            var themeExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = themeExpanded,
                                onExpandedChange = { themeExpanded = !themeExpanded }
                            ) {
                                OutlinedTextField(
                                    value = uiState.appSettings.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = themeExpanded,
                                    onDismissRequest = { themeExpanded = false }
                                ) {
                                    for (mode in com.polaralias.signalsynthesis.data.settings.ThemeMode.values()) {
                                        DropdownMenuItem(
                                            text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                            onClick = {
                                                onUpdateSettings(uiState.appSettings.copy(themeMode = mode))
                                                themeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SectionHeader("API AUTHENTICATION")
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            val providerStatus = if (uiState.hasAnyApiKeys) "ACTIVE" else "DATA LINK OFFLINE"
                            val llmStatus = if (uiState.hasLlmKey) "ACTIVE" else "AI SERVICE OFFLINE"
                            
                            AuthStatusItem("MARKET DATA PROTOCOL", providerStatus, uiState.hasAnyApiKeys)
                            Spacer(modifier = Modifier.height(20.dp))
                            AuthStatusItem("AI ANALYSIS SERVICE", llmStatus, uiState.hasLlmKey)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                com.polaralias.signalsynthesis.ui.components.GlassBox(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clickable { onEditKeys() }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("RECONFIGURE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary, letterSpacing = 1.sp)
                                    }
                                }
                                
                                com.polaralias.signalsynthesis.ui.components.GlassBox(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clickable { onClearKeys() }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("PURGE ALL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = ErrorRed.copy(alpha = 0.6f), letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    SectionHeader("DAILY TELEMETRY USAGE")
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("PROTOCOL TRANSACTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    Text("Rolling 24h window", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                }
                                Text(uiState.dailyApiUsage.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = BrandPrimary)
                            }
                        }
                    }
                    
                    if (uiState.dailyProviderUsage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        uiState.dailyProviderUsage.forEach { (provider, categories) ->
                            val totalForProvider = categories.values.sum()
                            com.polaralias.signalsynthesis.ui.components.GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = provider.uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = BrandSecondary,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "$totalForProvider",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    categories.forEach { (category, count) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = formatCategoryName(category).uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
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

                    Spacer(modifier = Modifier.height(32.dp))
                    SectionHeader("AI CONFIGURATION")
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            var providerExpanded by remember { mutableStateOf(false) }
                            var analysisModelExpanded by remember { mutableStateOf(false) }
                            var verdictModelExpanded by remember { mutableStateOf(false) }
                            var reasoningModelExpanded by remember { mutableStateOf(false) }

                            Text("GLOBAL PROVIDER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = providerExpanded,
                                onExpandedChange = { providerExpanded = !providerExpanded }
                            ) {
                                OutlinedTextField(
                                    value = if (uiState.appSettings.llmProvider == LlmProvider.OPENAI) "OpenAI" else "Google Gemini",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = BrandPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = providerExpanded,
                                    onDismissRequest = { providerExpanded = false }
                                ) {
                                    for (provider in LlmProvider.values()) {
                                        DropdownMenuItem(
                                            text = { Text(if(provider == LlmProvider.OPENAI) "OpenAI" else "Google Gemini") },
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

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("ANALYSIS MODEL (DATA INTERPRETATION)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = analysisModelExpanded,
                                onExpandedChange = { analysisModelExpanded = !analysisModelExpanded }
                            ) {
                                OutlinedTextField(
                                    value = formatModelName(uiState.appSettings.analysisModel),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = analysisModelExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
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

                            Spacer(modifier = Modifier.height(20.dp))

                            Text("VERDICT MODEL (FINAL THESIS)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = verdictModelExpanded,
                                onExpandedChange = { verdictModelExpanded = !verdictModelExpanded }
                            ) {
                                OutlinedTextField(
                                    value = formatModelName(uiState.appSettings.verdictModel),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = verdictModelExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
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

                            Spacer(modifier = Modifier.height(20.dp))

                            Text("REASONING MODEL (DEEP SEARCH)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = reasoningModelExpanded,
                                onExpandedChange = { reasoningModelExpanded = !reasoningModelExpanded }
                            ) {
                                OutlinedTextField(
                                    value = formatModelName(uiState.appSettings.reasoningModel),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasoningModelExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
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

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp).alpha(0.1f),
                                color = BrandPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("QUANTITATIVE TUNING")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Reasoning Depth / Thinking Budget
                    val isGemini = uiState.appSettings.llmProvider == LlmProvider.GEMINI
                    val isThinkingModel = uiState.appSettings.analysisModel.name.contains("GEMINI_3") || 
                                         uiState.appSettings.analysisModel.name.contains("GPT_5")
                    
                    Text(
                        text = if (isGemini) "THINKING LEVEL" else "REASONING EFFORT", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Black, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), 
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var reasoningExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = reasoningExpanded,
                        onExpandedChange = { reasoningExpanded = !reasoningExpanded }
                    ) {
                        OutlinedTextField(
                            value = when (uiState.appSettings.reasoningDepth) {
                                ReasoningDepth.NONE -> if (isGemini) (if (uiState.appSettings.analysisModel.name.contains("FLASH")) "Minimal Thinking (Fastest)" else "Low Thinking Effort") else "No Effort (None)"
                                ReasoningDepth.MINIMAL -> if (isGemini) (if (uiState.appSettings.analysisModel.name.contains("FLASH")) "Minimal Thinking" else "Low Thinking Effort") else "Minimal Effort"
                                ReasoningDepth.LOW -> if (isGemini) "Low Thinking Effort" else "Low Effort"
                                ReasoningDepth.MEDIUM -> if (isGemini) (if (uiState.appSettings.analysisModel.name.contains("FLASH")) "Medium Thinking" else "High Thinking (Default)") else "Medium Effort"
                                ReasoningDepth.HIGH -> if (isGemini) "High Thinking (Dynamic)" else "High Effort"
                                ReasoningDepth.EXTRA -> if (isGemini) "Maximum Thinking" else "Extreme Effort (X-High)"
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasoningExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = reasoningExpanded,
                            onDismissRequest = { reasoningExpanded = false }
                        ) {
                            val currentModel = uiState.appSettings.analysisModel
                            val isFlash = currentModel.name.contains("FLASH")
                            val isPro = currentModel.name.contains("PRO") && currentModel.name.contains("GEMINI_3")

                            for (depth in com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.values()) {
                                // Model-specific constraints
                                val isExtraSupported = currentModel.name.contains("GPT_5_2") || 
                                                      currentModel.name.contains("PRO") ||
                                                      currentModel.name.contains("GEMINI_3")
                                
                                if (depth == com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.EXTRA && !isExtraSupported) continue

                                // Gemini Pro Specific Filtering: Only Low and High supported
                                if (isPro && (depth == ReasoningDepth.MINIMAL || depth == ReasoningDepth.MEDIUM)) continue

                                DropdownMenuItem(
                                    text = { 
                                        Text(when (depth) {
                                            ReasoningDepth.NONE -> if (isGemini) (if (isFlash) "Minimal (Fastest)" else "Low Effort") else "None"
                                            ReasoningDepth.MINIMAL -> if (isGemini) "Minimal" else "Minimal"
                                            ReasoningDepth.LOW -> if (isGemini) "Low" else "Low"
                                            ReasoningDepth.MEDIUM -> if (isGemini) "Medium / Balanced" else "Medium"
                                            ReasoningDepth.HIGH -> if (isGemini) "High (Dynamic)" else "High"
                                            ReasoningDepth.EXTRA -> if (isGemini) "Maximum" else "Extreme (X-High)"
                                        })
                                    },
                                    onClick = {
                                        onUpdateSettings(uiState.appSettings.copy(reasoningDepth = depth))
                                        reasoningExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("SYNTHESIS DURATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    var lengthExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = lengthExpanded,
                        onExpandedChange = { lengthExpanded = !lengthExpanded }
                    ) {
                        OutlinedTextField(
                            value = uiState.appSettings.outputLength.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lengthExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
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
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    val isGeminiVal = uiState.appSettings.llmProvider == LlmProvider.GEMINI
                    Text(
                        text = if (isGeminiVal) "VERBOSITY (OPENAI EXCLUSIVE)" else "VERBOSITY",
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Black,
                        color = if (isGeminiVal) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var verbosityExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = verbosityExpanded && !isGeminiVal,
                        onExpandedChange = { if (!isGeminiVal) verbosityExpanded = !verbosityExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (isGeminiVal) "Prompt-guided only" else uiState.appSettings.verbosity.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isGeminiVal,
                            trailingIcon = { if (!isGeminiVal) ExposedDropdownMenuDefaults.TrailingIcon(expanded = verbosityExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            supportingText = { if (isGeminiVal) Text("GEMINI MODELS USE INTERNAL PROMPT TUNING") }
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

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("RISK TOLERANCE PROFILE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    var riskExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = riskExpanded,
                        onExpandedChange = { riskExpanded = !riskExpanded }
                    ) {
                        OutlinedTextField(
                            value = uiState.appSettings.riskTolerance.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = riskExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
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
            
            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("AUTONOMOUS MONITORING")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIVE PROTOCOLS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = BrandPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (uiState.alertsEnabled) "SCANNING ${uiState.alertSymbolCount} ASSETS" else "MONITORING SUSPENDED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.alertsEnabled) BrandPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
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
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BrandPrimary,
                                checkedTrackColor = BrandPrimary.copy(alpha = 0.2f)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    SettingsSlider(
                        label = "POLLING LATENCY",
                        value = uiState.appSettings.alertCheckIntervalMinutes.toDouble(),
                        range = 1.0f..60.0f,
                        steps = 59,
                        format = { "${it.toInt()} MIN" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(alertCheckIntervalMinutes = it.toInt())) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("TECHNICAL TRIGGER THRESHOLDS")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    SettingsSlider(
                        label = "VWAP DIP TOLERANCE",
                        value = uiState.appSettings.vwapDipPercent,
                        range = 0.1f..5.0f,
                        steps = 49,
                        format = { String.format("%.1f%%", it) },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(vwapDipPercent = it)) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSlider(
                        label = "RSI OVERSOLD LIMIT",
                        value = uiState.appSettings.rsiOversold,
                        range = 10.0f..50.0f,
                        steps = 40,
                        format = { it.roundToInt().toString() },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(rsiOversold = it)) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSlider(
                        label = "RSI OVERBOUGHT LIMIT",
                        value = uiState.appSettings.rsiOverbought,
                        range = 50.0f..90.0f,
                        steps = 40,
                        format = { it.roundToInt().toString() },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(rsiOverbought = it)) }
                    )
                    
                    if (uiState.aiThresholdSuggestion != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        AiSuggestionCard(
                            title = "AI THRESHOLD OPTIMIZATION",
                            suggestionText = "VWAP: ${String.format("%.1f%%", uiState.aiThresholdSuggestion.vwapDipPercent)} | RSI: ${uiState.aiThresholdSuggestion.rsiOversold.roundToInt()}/${uiState.aiThresholdSuggestion.rsiOverbought.roundToInt()}",
                            rationale = uiState.aiThresholdSuggestion.rationale,
                            onApply = onApplyAi,
                            onDismiss = onDismissAi
                        )
                    } else {
                        Spacer(modifier = Modifier.height(32.dp))
                        com.polaralias.signalsynthesis.ui.components.GlassBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable(enabled = uiState.hasLlmKey && !uiState.isSuggestingThresholds) { showThresholdAiDialog = true }
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoMode, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(18.dp), 
                                        tint = BrandSecondary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        if (uiState.isSuggestingThresholds) "SYNTHESIZING..." else "OPTIMIZE WITH AI", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        fontWeight = FontWeight.Black, 
                                        color = BrandSecondary,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }
                }

            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("SCREENER PARAMETERS")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    SettingsSlider(
                        label = "CONSERVATIVE CAP",
                        value = uiState.appSettings.screenerConservativeThreshold,
                        range = 1.0f..100.0f,
                        steps = 99,
                        format = { "$${it.roundToInt()}" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerConservativeThreshold = it)) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSlider(
                        label = "MODERATE CAP",
                        value = uiState.appSettings.screenerModerateThreshold,
                        range = 1.0f..500.0f,
                        steps = 499,
                        format = { "$${it.roundToInt()}" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerModerateThreshold = it)) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSlider(
                        label = "AGGRESSIVE CAP",
                        value = uiState.appSettings.screenerAggressiveThreshold,
                        range = 1.0f..2000.0f,
                        steps = 1999,
                        format = { "$${it.roundToInt()}" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerAggressiveThreshold = it)) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSlider(
                        label = "MINIMUM LIQUIDITY",
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
                        Spacer(modifier = Modifier.height(32.dp))
                        AiSuggestionCard(
                            title = "AI SCREENER OPTIMIZATION",
                            suggestionText = "CONS: $${uiState.aiScreenerSuggestion.conservativeLimit.roundToInt()} | MOD: $${uiState.aiScreenerSuggestion.moderateLimit.roundToInt()} | AGGR: $${uiState.aiScreenerSuggestion.aggressiveLimit.roundToInt()} | VOL: ${String.format("%.1fM", uiState.aiScreenerSuggestion.minVolume / 1_000_000.0)}",
                            rationale = uiState.aiScreenerSuggestion.rationale,
                            onApply = onApplyScreenerAi,
                            onDismiss = onDismissScreenerAi
                        )
                    } else {
                        Spacer(modifier = Modifier.height(32.dp))
                        com.polaralias.signalsynthesis.ui.components.GlassBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable(enabled = uiState.hasLlmKey && !uiState.isSuggestingScreener) { showScreenerAiDialog = true }
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoMode, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(18.dp), 
                                        tint = BrandSecondary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        if (uiState.isSuggestingScreener) "OPTIMIZING..." else "OPTIMIZE SCREENER", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        fontWeight = FontWeight.Black, 
                                        color = BrandSecondary,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }
                }

            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("SYSTEM OPERATIONS")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("TELEMETRY LOGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Real-time traffic audit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                        TextButton(onClick = onOpenLogs) {
                            Text("ACCESS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("CUSTOM TICKERS")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = newTicker,
                        onValueChange = { 
                            newTicker = it.uppercase()
                            onSearchTickers(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("SEARCH OR ENTER SYMBOL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        ),
                        trailingIcon = {
                            if (newTicker.isNotBlank()) {
                                IconButton(onClick = { 
                                    onAddCustomTicker(newTicker)
                                    newTicker = ""
                                    onClearTickerSearch()
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = BrandPrimary)
                                }
                            }
                        }
                    )
                    
                    if (uiState.tickerSearchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            LazyColumn {
                                items(uiState.tickerSearchResults) { result ->
                                    androidx.compose.material3.ListItem(
                                        headlineContent = { Text(result.symbol, fontWeight = FontWeight.Black, color = BrandPrimary) },
                                        supportingContent = { Text(result.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
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
                    }
                    
                    if (uiState.customTickers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        uiState.customTickers.forEach { ticker ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(ticker.symbol, fontWeight = FontWeight.Black, color = BrandPrimary, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    SourceBadge(ticker.source)
                                }
                                IconButton(onClick = { onRemoveCustomTicker(ticker.symbol) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", modifier = Modifier.size(18.dp), tint = ErrorRed.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("MARKET DATA FEEDS")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("RSS/ATOM SOURCES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (uiState.appSettings.rssFeeds.isEmpty()) {
                        Text("No custom feeds configured.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    } else {
                        uiState.appSettings.rssFeeds.forEach { feedUrl ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.RssFeed, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(feedUrl, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                }
                                IconButton(onClick = { onRemoveRssFeed(feedUrl) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Box {
                        OutlinedTextField(
                            value = newRssFeed,
                            onValueChange = { newRssFeed = it },
                            label = { Text("ADD FEED URL", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth().padding(end = 50.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            enabled = !isAddingRss,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = BrandPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        )
                        if (isAddingRss) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterEnd).size(24.dp).padding(4.dp), color = BrandPrimary)
                        } else {
                            IconButton(
                                onClick = { 
                                    if (newRssFeed.isNotBlank()) {
                                        isAddingRss = true
                                        rssAddResult = null
                                        keyboardController?.hide()
                                        onAddRssFeed(newRssFeed) { success, message ->
                                            isAddingRss = false
                                            rssAddResult = message
                                            if (success) {
                                                newRssFeed = ""
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = BrandPrimary)
                            }
                        }
                    }
                    rssAddResult?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.labelSmall, color = if (it.startsWith("Added")) BrandPrimary else ErrorRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("BLACKLISTED NODES")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (uiState.blocklist.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                            Text("NO NODES SEGREGATED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        }
                    } else {
                        uiState.blocklist.forEach { symbol ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(symbol, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                TextButton(onClick = { onRemoveFromBlocklist(symbol) }) {
                                    Text("RESTORE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}
}

    if (showThresholdAiDialog) {
        AiPromptDialog(
            title = "THRESHOLD TUNING",
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
            title = "SCREENER TUNING",
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
        title = { 
            Text(
                title, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ) 
        },
        text = {
            Column {
                Text(
                    "Describe your trading archetype or risk profile (e.g., 'Aggressive day trader seeking high-velocity momentum').",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = aiPrompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Neural context...") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("SYNTHESIZE", color = BrandPrimary, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontWeight = FontWeight.Black)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = label, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = format(value), 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Black,
                color = BrandPrimary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = BrandPrimary,
                activeTrackColor = BrandPrimary,
                inactiveTrackColor = BrandPrimary.copy(alpha = 0.1f)
            )
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
    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = BrandSecondary,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = suggestionText, 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = BrandSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "NEURAL RATIONALE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.polaralias.signalsynthesis.ui.components.GlassBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { onApply() }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("APPLY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandSecondary)
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("DISCARD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }
}

private fun formatModelName(model: LlmModel): String {
    return when (model) {
        LlmModel.GPT_5_2 -> "GPT 5.2"
        LlmModel.GPT_5_1 -> "GPT 5.1"
        LlmModel.GPT_5_MINI -> "GPT 5 MINI"
        LlmModel.GPT_5_NANO -> "GPT 5 NANO"
        LlmModel.GEMINI_3_FLASH -> "GEMINI 3 FLASH"
        LlmModel.GEMINI_3_PRO -> "GEMINI 3 PRO"
        else -> model.name.replace("_", " ")
    }
}

private fun formatCategoryName(category: com.polaralias.signalsynthesis.util.ApiUsageCategory): String {
    return when (category) {
        com.polaralias.signalsynthesis.util.ApiUsageCategory.DISCOVERY -> "Discovery"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.ANALYSIS -> "Synthesis"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.FUNDAMENTALS -> "Fundamentals"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.ALERTS -> "Monitoring"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.SEARCH -> "Network Search"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.OTHER -> "Protocols"
    }
}

