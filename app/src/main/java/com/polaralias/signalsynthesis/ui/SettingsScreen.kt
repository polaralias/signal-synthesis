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
import com.polaralias.signalsynthesis.data.rss.RssFeedDefaults
import com.polaralias.signalsynthesis.data.rss.RssFeedTier
import com.polaralias.signalsynthesis.domain.ai.*
import com.polaralias.signalsynthesis.domain.model.AnalysisStage
import com.polaralias.signalsynthesis.ui.theme.*
import kotlin.math.roundToInt
import androidx.compose.ui.platform.testTag
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    onSuggestSettingsAi: (String, Set<AiSettingsArea>) -> Unit,
    onApplyAllAiSettings: () -> Unit,
    onApplyAi: () -> Unit,
    onDismissAi: () -> Unit,
    onOpenLogs: () -> Unit,
    onClearCaches: () -> Unit = {},
    onAddCustomTicker: (String) -> Unit,
    onRemoveCustomTicker: (String) -> Unit,
    onSearchTickers: (String) -> Unit,
    onClearTickerSearch: () -> Unit,
    onApplyScreenerAi: () -> Unit,
    onDismissScreenerAi: () -> Unit,
    onApplyRiskAi: () -> Unit,
    onDismissRiskAi: () -> Unit,
    onApplyRssAi: () -> Unit,
    onDismissRssAi: () -> Unit,
    onRemoveFromBlocklist: (String) -> Unit,
    onUpdateStageConfig: (AnalysisStage, StageModelConfig) -> Unit = { _, _ -> },
    onArchiveUsage: () -> Unit = {},
    onToggleRssTopic: (String) -> Unit = {},
    onToggleRssTickerSource: (String) -> Unit = {},
    onUpdateRssUseTickerFeedsForFinalStage: (Boolean) -> Unit = {},
    onUpdateRssApplyExpandedToAll: (Boolean) -> Unit = {},
    onResetRssDefaults: () -> Unit = {},
    onRequestRssPreview: (String) -> Unit = {}
) {
    var showSettingsAiDialog by remember { mutableStateOf(false) }
    var newTicker by remember { mutableStateOf("") }
    var rssSearchQuery by remember { mutableStateOf("") }
    val rssExpandedProviders = remember { mutableStateMapOf<String, Boolean>() }
    var rssPreviewSource by remember { mutableStateOf<com.polaralias.signalsynthesis.data.rss.RssFeedSource?>(null) }
    var rssPreviewTopicId by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    val monthlyUsageTotal = uiState.dailyApiUsage + uiState.archivedUsage.sumOf { it.totalCalls }
    val hasPendingAiSuggestions = uiState.aiThresholdSuggestion != null ||
        uiState.aiScreenerSuggestion != null ||
        uiState.aiRiskSuggestion != null ||
        uiState.aiRssSuggestion != null
    val aiSuggestedSettingsLocked = uiState.appSettings.aiSuggestedSettingsLocked
    var pendingAiOverrideSection by remember { mutableStateOf<String?>(null) }
    var expandGeneral by remember { mutableStateOf(true) }
    var expandAi by remember { mutableStateOf(false) }
    var expandMonitoring by remember { mutableStateOf(false) }
    var expandRiskAndDiscovery by remember { mutableStateOf(false) }
    var expandSystem by remember { mutableStateOf(false) }

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
                    SectionHeader("AI SETTINGS SUGGESTIONS")
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoMode, contentDescription = null, tint = BrandSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "SUGGEST ALL SETTINGS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = BrandSecondary,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select areas and generate AI recommendations. Apply to lock AI-managed settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            com.polaralias.signalsynthesis.ui.components.GlassBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable(enabled = uiState.hasLlmKey && !uiState.isSuggestingSettings) { showSettingsAiDialog = true }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (uiState.isSuggestingSettings) "SYNTHESIZING..." else "SUGGEST ALL SETTINGS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (uiState.hasLlmKey) BrandSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            if (uiState.isSuggestingSettings) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = BrandSecondary,
                                    trackColor = BrandSecondary.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.settingsSuggestionProgress ?: "Preparing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            } else if (uiState.settingsSuggestionCompletedAt != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Completed at ${formatTime(uiState.settingsSuggestionCompletedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrandPrimary
                                    )
                                }
                                uiState.settingsSuggestionExplanation?.takeIf { it.isNotBlank() }?.let { explanation ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            if (!uiState.hasLlmKey) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add an AI key to enable suggestions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            if (uiState.lastAiSettingsPrompt.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "LAST PROMPT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.lastAiSettingsPrompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 3
                                )
                            }
                            if (hasPendingAiSuggestions) {
                                Spacer(modifier = Modifier.height(16.dp))
                                com.polaralias.signalsynthesis.ui.components.GlassBox(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clickable { onApplyAllAiSettings() }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            "APPLY ALL AI SUGGESTIONS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = BrandSecondary,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                            if (aiSuggestedSettingsLocked) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "AI-managed settings are locked. Tap a locked section to override.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandSecondary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader("SETTINGS SECTIONS")
                    SettingsAccordionHeader(
                        title = "GENERAL",
                        subtitle = "Appearance, auth, offline mode, usage",
                        expanded = expandGeneral,
                        onToggle = { expandGeneral = !expandGeneral }
                    )
                    SettingsAccordionHeader(
                        title = "AI CONFIG",
                        subtitle = "Provider, models, routing and tuning",
                        expanded = expandAi,
                        onToggle = { expandAi = !expandAi }
                    )
                    SettingsAccordionHeader(
                        title = "NOTIFICATIONS",
                        subtitle = "Monitoring and polling controls",
                        expanded = expandMonitoring,
                        onToggle = { expandMonitoring = !expandMonitoring }
                    )
                    SettingsAccordionHeader(
                        title = "RISK & DISCOVERY",
                        subtitle = "Thresholds, screener, RSS and tickers",
                        expanded = expandRiskAndDiscovery,
                        onToggle = { expandRiskAndDiscovery = !expandRiskAndDiscovery }
                    )
                    SettingsAccordionHeader(
                        title = "SYSTEM",
                        subtitle = "Cache, logs, maintenance",
                        expanded = expandSystem,
                        onToggle = { expandSystem = !expandSystem }
                    )

                    if (expandGeneral) {
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
                    SectionHeader("OFFLINE FALLBACK")
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "USE MOCK DATA WHEN OFFLINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = BrandPrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        "Enable demo mode if no provider keys are configured.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                                Switch(
                                    checked = uiState.appSettings.useMockDataWhenOffline,
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings(uiState.appSettings.copy(useMockDataWhenOffline = enabled))
                                    },
                                    modifier = Modifier.testTag("mock_toggle"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BrandPrimary,
                                        checkedTrackColor = BrandPrimary.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                    }
                    
                    if (expandSystem) {
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
                                Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("MONTHLY AGGREGATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                        Text("Last 30 days (compare vs provider limits)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                    }
                                    Text(monthlyUsageTotal.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = BrandSecondary)
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
                    }

                    if (expandAi) {
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
                                    value = uiState.appSettings.llmProvider.displayName,
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
                                            text = { Text(provider.displayName) },
                                            onClick = {
                                                val providerModels = LlmModel.modelsForProvider(provider)
                                                val defaultModel = providerModels.firstOrNull {
                                                    it.visibilityGroup == LlmModelVisibilityGroup.CORE_REASONING
                                                } ?: providerModels.firstOrNull() ?: uiState.appSettings.analysisModel
                                                onUpdateSettings(uiState.appSettings.copy(
                                                    llmProvider = provider,
                                                    analysisModel = defaultModel,
                                                    verdictModel = defaultModel,
                                                    reasoningModel = defaultModel,
                                                    deepDiveProvider = provider
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
                                    val groupedModels = groupedModelsForProvider(uiState.appSettings.llmProvider)
                                    groupedModels.forEach { (groupLabel, models) ->
                                        DropdownMenuItem(
                                            enabled = false,
                                            text = { Text(groupLabel.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                            onClick = {}
                                        )
                                        models.forEach { model ->
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
                                    val groupedModels = groupedModelsForProvider(uiState.appSettings.llmProvider)
                                    groupedModels.forEach { (groupLabel, models) ->
                                        DropdownMenuItem(
                                            enabled = false,
                                            text = { Text(groupLabel.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                            onClick = {}
                                        )
                                        models.forEach { model ->
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
                                    val groupedModels = groupedModelsForProvider(uiState.appSettings.llmProvider)
                                    groupedModels.forEach { (groupLabel, models) ->
                                        DropdownMenuItem(
                                            enabled = false,
                                            text = { Text(groupLabel.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                            onClick = {}
                                        )
                                        models.forEach { model ->
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

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp).alpha(0.1f),
                                color = BrandPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("STAGE ROUTING OVERRIDES")
                    com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Assign provider/model per pipeline stage. Global AI settings are used only when a stage has no override.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            var stageProviderExpanded by remember { mutableStateOf<AnalysisStage?>(null) }
                            var stageModelExpanded by remember { mutableStateOf<AnalysisStage?>(null) }
                            var stageToolsExpanded by remember { mutableStateOf<AnalysisStage?>(null) }
                            val stageOrder = listOf(
                                AnalysisStage.RSS_VERIFY,
                                AnalysisStage.SHORTLIST,
                                AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS,
                                AnalysisStage.DECISION_UPDATE,
                                AnalysisStage.DEEP_DIVE
                            )

                            stageOrder.forEachIndexed { index, stage ->
                                val stageConfig = uiState.appSettings.modelRouting.getConfigForStage(stage)

                                Text(
                                    text = stageDisplayName(stage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = BrandPrimary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stageDescription(stage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "PROVIDER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ExposedDropdownMenuBox(
                                    expanded = stageProviderExpanded == stage,
                                    onExpandedChange = {
                                        stageProviderExpanded = if (stageProviderExpanded == stage) null else stage
                                    }
                                ) {
                                    OutlinedTextField(
                                        value = stageConfig.provider.displayName,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageProviderExpanded == stage)
                                        },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = stageProviderExpanded == stage,
                                        onDismissRequest = { stageProviderExpanded = null }
                                    ) {
                                        LlmProvider.values().forEach { provider ->
                                            DropdownMenuItem(
                                                text = { Text(provider.displayName) },
                                                onClick = {
                                                    val providerModels = LlmModel.modelsForProvider(provider)
                                                    val defaultModel = providerModels.firstOrNull {
                                                        it.visibilityGroup == LlmModelVisibilityGroup.CORE_REASONING
                                                    } ?: providerModels.firstOrNull()
                                                    if (defaultModel != null) {
                                                        val updatedTools = when {
                                                            stage != AnalysisStage.DEEP_DIVE -> ToolsMode.NONE
                                                            !provider.supportsWebTools() -> ToolsMode.NONE
                                                            stageConfig.tools == ToolsMode.NONE -> ToolsMode.NONE
                                                            provider == LlmProvider.GEMINI -> ToolsMode.GOOGLE_SEARCH
                                                            else -> ToolsMode.WEB_SEARCH
                                                        }
                                                        onUpdateStageConfig(
                                                            stage,
                                                            stageConfig.copy(
                                                                provider = provider,
                                                                model = defaultModel.modelId,
                                                                tools = updatedTools
                                                            )
                                                        )
                                                    }
                                                    stageProviderExpanded = null
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    "MODEL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ExposedDropdownMenuBox(
                                    expanded = stageModelExpanded == stage,
                                    onExpandedChange = {
                                        stageModelExpanded = if (stageModelExpanded == stage) null else stage
                                    }
                                ) {
                                    OutlinedTextField(
                                        value = formatStageModelLabel(stageConfig.provider, stageConfig.model),
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageModelExpanded == stage)
                                        },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = stageModelExpanded == stage,
                                        onDismissRequest = { stageModelExpanded = null }
                                    ) {
                                        val groupedModels = groupedModelsForProvider(stageConfig.provider)
                                        groupedModels.forEach { (groupLabel, models) ->
                                            DropdownMenuItem(
                                                enabled = false,
                                                text = { Text(groupLabel.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                                onClick = {}
                                            )
                                            models.forEach { model ->
                                                DropdownMenuItem(
                                                    text = { Text(formatModelName(model)) },
                                                    onClick = {
                                                        onUpdateStageConfig(
                                                            stage,
                                                            stageConfig.copy(model = model.modelId)
                                                        )
                                                        stageModelExpanded = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (stage == AnalysisStage.DEEP_DIVE) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "LIVE WEB SOURCES",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    ExposedDropdownMenuBox(
                                        expanded = stageToolsExpanded == stage,
                                        onExpandedChange = {
                                            stageToolsExpanded = if (stageToolsExpanded == stage) null else stage
                                        }
                                    ) {
                                        OutlinedTextField(
                                            value = if (stageConfig.tools == ToolsMode.NONE) "Disabled" else "Enabled",
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageToolsExpanded == stage)
                                            },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = stageToolsExpanded == stage,
                                            onDismissRequest = { stageToolsExpanded = null }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Disabled") },
                                                onClick = {
                                                    onUpdateStageConfig(stage, stageConfig.copy(tools = ToolsMode.NONE))
                                                    stageToolsExpanded = null
                                                }
                                            )
                                            DropdownMenuItem(
                                                enabled = stageConfig.provider.supportsWebTools(),
                                                text = {
                                                    Text(
                                                        if (stageConfig.provider == LlmProvider.GEMINI) {
                                                            "Enabled (Google Search)"
                                                        } else {
                                                            "Enabled (Web Search)"
                                                        }
                                                    )
                                                },
                                                onClick = {
                                                    val toolsMode = if (stageConfig.provider == LlmProvider.GEMINI) {
                                                        ToolsMode.GOOGLE_SEARCH
                                                    } else {
                                                        ToolsMode.WEB_SEARCH
                                                    }
                                                    onUpdateStageConfig(stage, stageConfig.copy(tools = toolsMode))
                                                    stageToolsExpanded = null
                                                }
                                            )
                                        }
                                    }
                                    if (!stageConfig.provider.supportsWebTools()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Current provider does not support native web search tools for this stage.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }

                                if (index < stageOrder.lastIndex) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("QUANTITATIVE TUNING")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val activeProvider = uiState.appSettings.llmProvider
                    val activeModelId = uiState.appSettings.analysisModel.modelId
                    val normalizedModelId = LlmModel.normalizeModelIdAlias(activeModelId).lowercase()
                    val supportsNativeReasoning = LlmModel.supportsNativeReasoningControl(activeProvider, activeModelId)
                    val isGemini3 = activeProvider == LlmProvider.GEMINI && normalizedModelId.startsWith("gemini-3")
                    val isGemini3Flash = isGemini3 && normalizedModelId.contains("flash")

                    Text(
                        text = when {
                            activeProvider == LlmProvider.GEMINI && supportsNativeReasoning -> "THINKING LEVEL"
                            supportsNativeReasoning -> "REASONING EFFORT"
                            else -> "REASONING DEPTH (PROMPT GUIDANCE)"
                        },
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
                            value = formatReasoningDepthLabel(
                                depth = uiState.appSettings.reasoningDepth,
                                provider = activeProvider,
                                modelId = activeModelId
                            ),
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
                            for (depth in com.polaralias.signalsynthesis.domain.ai.ReasoningDepth.values()) {
                                // Gemini 3 Pro currently supports low/high levels only.
                                if (isGemini3 && !isGemini3Flash && (depth == ReasoningDepth.MINIMAL || depth == ReasoningDepth.MEDIUM)) {
                                    continue
                                }

                                DropdownMenuItem(
                                    text = { Text(formatReasoningDepthLabel(depth, activeProvider, activeModelId)) },
                                    onClick = {
                                        onUpdateSettings(uiState.appSettings.copy(reasoningDepth = depth))
                                        reasoningExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!supportsNativeReasoning) {
                        Text(
                            text = "Applied via prompt strategy for this provider/model.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isGeminiVal) "VERBOSITY (OPENAI EXCLUSIVE)" else "VERBOSITY",
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = FontWeight.Black,
                            color = if (isGeminiVal) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            letterSpacing = 1.sp
                        )
                        if (isGeminiVal) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text("Gemini does not support verbosity controls.") } },
                                state = rememberTooltipState()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Verbosity not supported for Gemini",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
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

                    Text("AI SUMMARY PREFETCH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (uiState.appSettings.aiSummaryPrefetchEnabled) "Prefetch Enabled" else "Prefetch Disabled",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = if (uiState.appSettings.aiSummaryPrefetchEnabled) BrandSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Text(
                                "Automatically pre-compute summaries for top setups.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        Switch(
                            checked = uiState.appSettings.aiSummaryPrefetchEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSettings(uiState.appSettings.copy(aiSummaryPrefetchEnabled = enabled))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BrandSecondary,
                                checkedTrackColor = BrandSecondary.copy(alpha = 0.2f)
                            )
                        )
                    }

                    if (uiState.appSettings.aiSummaryPrefetchEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsSlider(
                            label = "PREFETCH LIMIT",
                            value = uiState.appSettings.aiSummaryPrefetchLimit.toDouble(),
                            range = 1.0f..10.0f,
                            steps = 8,
                            format = { "${it.toInt()} SETUPS" },
                            onValueChange = { onUpdateSettings(uiState.appSettings.copy(aiSummaryPrefetchLimit = it.toInt().coerceAtLeast(1))) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("RISK TOLERANCE PROFILE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (aiSuggestedSettingsLocked) {
                        com.polaralias.signalsynthesis.ui.components.GlassBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { pendingAiOverrideSection = "risk profile" }
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                Text(
                                    text = "AI suggested. Tap to override before editing.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    } else {
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
            }
                    }
            
            if (expandMonitoring) {
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
                    if (uiState.appSettings.alertCheckIntervalMinutes <= 5) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Warning: shorter polling intervals can increase API usage and quota consumption.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            }
            
            if (expandRiskAndDiscovery) {
            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("TECHNICAL TRIGGER THRESHOLDS")
            AiLockedCard(
                locked = aiSuggestedSettingsLocked,
                lockMessage = "AI suggested thresholds. Tap to override.",
                onLockedClick = { pendingAiOverrideSection = "threshold settings" }
            ) {
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    SettingsSlider(
                        label = "VWAP DIP TOLERANCE",
                        value = uiState.appSettings.vwapDipPercent,
                        range = 0.1f..5.0f,
                        steps = 49,
                        format = { String.format(Locale.US, "%.1f%%", it) },
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
                    
                    val thresholdSuggestion = uiState.aiThresholdSuggestion
                    val lastThresholdSuggestion = uiState.lastAiThresholdSuggestion
                    if (thresholdSuggestion != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        AiSuggestionCard(
                            title = "AI THRESHOLD OPTIMIZATION",
                            suggestionText = "VWAP: ${String.format(Locale.US, "%.1f%%", thresholdSuggestion.vwapDipPercent)} | RSI: ${thresholdSuggestion.rsiOversold.roundToInt()}/${thresholdSuggestion.rsiOverbought.roundToInt()}",
                            rationale = thresholdSuggestion.rationale,
                            onApply = onApplyAi,
                            onDismiss = onDismissAi
                        )
                    } else if (lastThresholdSuggestion != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        AiSuggestionCard(
                            title = "LAST AI THRESHOLD",
                            suggestionText = "VWAP: ${String.format(Locale.US, "%.1f%%", lastThresholdSuggestion.vwapDipPercent)} | RSI: ${lastThresholdSuggestion.rsiOversold.roundToInt()}/${lastThresholdSuggestion.rsiOverbought.roundToInt()}",
                            rationale = lastThresholdSuggestion.rationale,
                            onApply = onApplyAi,
                            onDismiss = onDismissAi,
                            applyLabel = "APPLY LAST",
                            dismissLabel = "CLEAR"
                        )
                    } else {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Run \"Suggest all settings\" to generate AI threshold tuning.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("SCREENER PARAMETERS")
            AiLockedCard(
                locked = aiSuggestedSettingsLocked,
                lockMessage = "AI suggested screener values. Tap to override.",
                onLockedClick = { pendingAiOverrideSection = "screener settings" }
            ) {
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
                            if (vol >= 1_000_000) String.format(Locale.US, "%.1fM", vol / 1_000_000.0)
                            else String.format(Locale.US, "%dK", vol / 1_000)
                        },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(screenerMinVolume = it.toLong())) }
                    )
                    
                    val screenerSuggestion = uiState.aiScreenerSuggestion
                    val lastScreenerSuggestion = uiState.lastAiScreenerSuggestion
                    if (screenerSuggestion != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        AiSuggestionCard(
                            title = "AI SCREENER OPTIMIZATION",
                            suggestionText = "CONS: $${screenerSuggestion.conservativeLimit.roundToInt()} | MOD: $${screenerSuggestion.moderateLimit.roundToInt()} | AGGR: $${screenerSuggestion.aggressiveLimit.roundToInt()} | VOL: ${String.format(Locale.US, "%.1fM", screenerSuggestion.minVolume / 1_000_000.0)}",
                            rationale = screenerSuggestion.rationale,
                            onApply = onApplyScreenerAi,
                            onDismiss = onDismissScreenerAi
                        )
                    } else if (lastScreenerSuggestion != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        AiSuggestionCard(
                            title = "LAST AI SCREENER",
                            suggestionText = "CONS: $${lastScreenerSuggestion.conservativeLimit.roundToInt()} | MOD: $${lastScreenerSuggestion.moderateLimit.roundToInt()} | AGGR: $${lastScreenerSuggestion.aggressiveLimit.roundToInt()} | VOL: ${String.format(Locale.US, "%.1fM", lastScreenerSuggestion.minVolume / 1_000_000.0)}",
                            rationale = lastScreenerSuggestion.rationale,
                            onApply = onApplyScreenerAi,
                            onDismiss = onDismissScreenerAi,
                            applyLabel = "APPLY LAST",
                            dismissLabel = "CLEAR"
                        )
                    } else {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Run \"Suggest all settings\" to generate AI screener parameters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            }
            }
            
            if (expandSystem) {
            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("CACHE CONTROL")
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    SettingsSlider(
                        label = "QUOTE CACHE TTL",
                        value = uiState.appSettings.cacheTtlQuotesMinutes.toDouble(),
                        range = 1.0f..10.0f,
                        steps = 9,
                        format = { "${it.toInt()} MIN" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(cacheTtlQuotesMinutes = it.toInt().coerceAtLeast(1))) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    SettingsSlider(
                        label = "INTRADAY CACHE TTL",
                        value = uiState.appSettings.cacheTtlIntradayMinutes.toDouble(),
                        range = 1.0f..60.0f,
                        steps = 59,
                        format = { "${it.toInt()} MIN" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(cacheTtlIntradayMinutes = it.toInt().coerceAtLeast(1))) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    SettingsSlider(
                        label = "DAILY CACHE TTL",
                        value = uiState.appSettings.cacheTtlDailyMinutes.toDouble(),
                        range = 60.0f..1440.0f,
                        steps = 23,
                        format = { "${(it / 60).roundToInt()} H" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(cacheTtlDailyMinutes = it.toInt().coerceAtLeast(60))) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    SettingsSlider(
                        label = "PROFILE CACHE TTL",
                        value = uiState.appSettings.cacheTtlProfileMinutes.toDouble(),
                        range = 60.0f..1440.0f,
                        steps = 23,
                        format = { "${(it / 60).roundToInt()} H" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(cacheTtlProfileMinutes = it.toInt().coerceAtLeast(60))) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    SettingsSlider(
                        label = "METRICS CACHE TTL",
                        value = uiState.appSettings.cacheTtlMetricsMinutes.toDouble(),
                        range = 60.0f..1440.0f,
                        steps = 23,
                        format = { "${(it / 60).roundToInt()} H" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(cacheTtlMetricsMinutes = it.toInt().coerceAtLeast(60))) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    SettingsSlider(
                        label = "SENTIMENT CACHE TTL",
                        value = uiState.appSettings.cacheTtlSentimentMinutes.toDouble(),
                        range = 5.0f..120.0f,
                        steps = 23,
                        format = { "${it.toInt()} MIN" },
                        onValueChange = { onUpdateSettings(uiState.appSettings.copy(cacheTtlSentimentMinutes = it.toInt().coerceAtLeast(5))) }
                    )
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
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("VERBOSE LOGGING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Toggle detailed request/response capture.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                        Switch(
                            checked = uiState.appSettings.verboseLogging,
                            onCheckedChange = { enabled ->
                                onUpdateSettings(uiState.appSettings.copy(verboseLogging = enabled))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BrandSecondary,
                                checkedTrackColor = BrandSecondary.copy(alpha = 0.2f)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    com.polaralias.signalsynthesis.ui.components.GlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onClearCaches() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "CLEAR IN-MEMORY CACHES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = BrandSecondary,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
            }

            if (expandRiskAndDiscovery) {
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
            }
            
            if (expandRiskAndDiscovery) {
            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.height(32.dp))
            SectionHeader("MARKET DATA FEEDS")
            AiLockedCard(
                locked = aiSuggestedSettingsLocked,
                lockMessage = "AI suggested RSS sources. Tap to override.",
                onLockedClick = { pendingAiOverrideSection = "rss feed settings" }
            ) {
            com.polaralias.signalsynthesis.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("CURATED RSS CATALOG", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val rssSuggestion = uiState.aiRssSuggestion
                    val lastRssSuggestion = uiState.lastAiRssSuggestion
                    if (rssSuggestion != null) {
                        AiSuggestionCard(
                            title = "AI RSS SELECTION",
                            suggestionText = formatRssSuggestionSummary(rssSuggestion, uiState.rssCatalog),
                            rationale = rssSuggestion.rationale,
                            onApply = onApplyRssAi,
                            onDismiss = onDismissRssAi
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    } else if (lastRssSuggestion != null) {
                        AiSuggestionCard(
                            title = "LAST AI RSS SELECTION",
                            suggestionText = formatRssSuggestionSummary(lastRssSuggestion, uiState.rssCatalog),
                            rationale = lastRssSuggestion.rationale,
                            onApply = onApplyRssAi,
                            onDismiss = onDismissRssAi,
                            applyLabel = "APPLY LAST",
                            dismissLabel = "CLEAR"
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        Text(
                            text = "Run \"Suggest all settings\" to get AI RSS curation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    RssToggleRow(
                        title = "ENABLE TICKER FEEDS FOR FINAL STAGE",
                        description = "Use ticker-specific sources when RSS is required for final setups.",
                        checked = uiState.appSettings.rssUseTickerFeedsForFinalStage,
                        onCheckedChange = onUpdateRssUseTickerFeedsForFinalStage
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RssToggleRow(
                        title = "APPLY EXPANDED FEEDS TO ALL DEEP DIVES",
                        description = "Overrides per-ticker gating for deep dives and web search.",
                        checked = uiState.appSettings.rssApplyExpandedToAll,
                        onCheckedChange = onUpdateRssApplyExpandedToAll
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(20.dp))

                    Text("TICKER FEED SOURCES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandSecondary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    val tickerSources = uiState.rssCatalog?.sources
                        ?.filter { source -> source.topics.any { it.isTickerTemplate } }
                        .orEmpty()

                    if (tickerSources.isEmpty()) {
                        Text("Catalog unavailable or no ticker sources found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    } else {
                        tickerSources.forEach { source ->
                            val enabled = uiState.appSettings.rssTickerSources.contains(source.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    source.label.uppercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (enabled) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onToggleRssTickerSource(source.id) }
                                )
                                Checkbox(checked = enabled, onCheckedChange = { onToggleRssTickerSource(source.id) })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onResetRssDefaults, modifier = Modifier.align(Alignment.End)) {
                        Text("RESET TO DEFAULTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandPrimary, letterSpacing = 1.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("TOPIC CATALOG", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandSecondary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = rssSearchQuery,
                        onValueChange = { rssSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("SEARCH PROVIDERS OR TOPICS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    val catalog = uiState.rssCatalog
                    if (catalog == null || catalog.sources.isEmpty()) {
                        Text("RSS catalog not loaded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    } else {
                        val query = rssSearchQuery.trim().lowercase()
                        catalog.sources.forEach { source ->
                            val topics = source.topics.filter { !it.isTickerTemplate }
                            val filteredTopics = if (query.isBlank()) {
                                topics
                            } else {
                                topics.filter { topic ->
                                    topic.label.lowercase().contains(query) || source.label.lowercase().contains(query)
                                }
                            }
                            if (filteredTopics.isNotEmpty()) {
                                val expanded = rssExpandedProviders[source.id] ?: false
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            rssExpandedProviders[source.id] = !expanded
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        source.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = BrandPrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            val previewTopics = source.topics.filter { !it.isTickerTemplate }
                                            if (previewTopics.isNotEmpty()) {
                                                rssPreviewSource = source
                                                rssPreviewTopicId = previewTopics.first().id
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            "PREVIEW",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = BrandSecondary,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = BrandPrimary
                                    )
                                }

                                val visibleTopics = if (expanded || query.isNotBlank()) {
                                    filteredTopics
                                } else {
                                    filteredTopics.take(6)
                                }

                                visibleTopics.forEach { topic ->
                                    val topicKey = "${source.id}:${topic.id}"
                                    val enabled = uiState.appSettings.rssEnabledTopics.contains(topicKey)
                                    val tier = RssFeedDefaults.tierFor(topicKey)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onToggleRssTopic(topicKey) }
                                        ) {
                                            Text(topic.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            if (tier != RssFeedTier.OTHER) {
                                                val tierColor = if (tier == RssFeedTier.CORE) BrandPrimary else BrandSecondary
                                                Text(
                                                    text = tier.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = tierColor.copy(alpha = 0.7f),
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                        }
                                        Checkbox(checked = enabled, onCheckedChange = { onToggleRssTopic(topicKey) })
                                    }
                                }

                                if (query.isBlank() && filteredTopics.size > visibleTopics.size) {
                                    TextButton(onClick = { rssExpandedProviders[source.id] = true }) {
                                        Text("SHOW ${filteredTopics.size - visibleTopics.size} MORE", style = MaterialTheme.typography.labelSmall, color = BrandSecondary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    val riskSuggestion = uiState.aiRiskSuggestion
                    val lastRiskSuggestion = uiState.lastAiRiskSuggestion
                    if (riskSuggestion != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        AiSuggestionCard(
                            title = "AI RISK PROFILE",
                            suggestionText = riskSuggestion.riskTolerance.name.lowercase().replaceFirstChar { it.uppercase() },
                            rationale = riskSuggestion.rationale,
                            onApply = onApplyRiskAi,
                            onDismiss = onDismissRiskAi
                        )
                    } else if (lastRiskSuggestion != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        AiSuggestionCard(
                            title = "LAST AI RISK PROFILE",
                            suggestionText = lastRiskSuggestion.riskTolerance.name.lowercase().replaceFirstChar { it.uppercase() },
                            rationale = lastRiskSuggestion.rationale,
                            onApply = onApplyRiskAi,
                            onDismiss = onDismissRiskAi,
                            applyLabel = "APPLY LAST",
                            dismissLabel = "CLEAR"
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Run \"Suggest all settings\" to get an AI risk profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            }
            }

            if (expandRiskAndDiscovery) {
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

    if (showSettingsAiDialog) {
        AiSettingsDialog(
            lastPrompt = uiState.lastAiSettingsPrompt,
            lastSelection = uiState.lastAiSettingsSelection,
            isBusy = uiState.isSuggestingSettings,
            onDismiss = { showSettingsAiDialog = false },
            onConfirm = { prompt, selected ->
                onSuggestSettingsAi(prompt, selected)
                showSettingsAiDialog = false
            }
        )
    }

    if (pendingAiOverrideSection != null) {
        AlertDialog(
            onDismissRequest = { pendingAiOverrideSection = null },
            title = {
                Text(
                    "AI SUGGESTED SETTINGS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Text(
                    "These ${pendingAiOverrideSection!!.lowercase()} values were applied from AI suggestions. Override and edit manually?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSettings(uiState.appSettings.copy(aiSuggestedSettingsLocked = false))
                        pendingAiOverrideSection = null
                    }
                ) {
                    Text("OVERRIDE", fontWeight = FontWeight.Black, color = BrandSecondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAiOverrideSection = null }) {
                    Text("CANCEL", fontWeight = FontWeight.Black)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    rssPreviewSource?.let { source ->
        RssPreviewDialog(
            source = source,
            selectedTopicId = rssPreviewTopicId,
            previewStates = uiState.rssPreviewStates,
            onSelectTopic = { topicId -> rssPreviewTopicId = topicId },
            onRequestPreview = onRequestRssPreview,
            onDismiss = {
                rssPreviewSource = null
                rssPreviewTopicId = null
            }
        )
    }
}
}


@Composable
private fun AiSettingsDialog(
    lastPrompt: String,
    lastSelection: Set<AiSettingsArea>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Set<AiSettingsArea>) -> Unit
) {
    val defaultSelection = if (lastSelection.isEmpty()) {
        setOf(AiSettingsArea.RSS, AiSettingsArea.RISK, AiSettingsArea.THRESHOLDS, AiSettingsArea.SCREENER)
    } else {
        lastSelection
    }
    var prompt by remember(lastPrompt) { mutableStateOf(lastPrompt) }
    var includeRss by remember(defaultSelection) { mutableStateOf(defaultSelection.contains(AiSettingsArea.RSS)) }
    var includeRisk by remember(defaultSelection) { mutableStateOf(defaultSelection.contains(AiSettingsArea.RISK)) }
    var includeThresholds by remember(defaultSelection) { mutableStateOf(defaultSelection.contains(AiSettingsArea.THRESHOLDS)) }
    var includeScreener by remember(defaultSelection) { mutableStateOf(defaultSelection.contains(AiSettingsArea.SCREENER)) }

    val selectedAreas = mutableSetOf<AiSettingsArea>().apply {
        if (includeRss) add(AiSettingsArea.RSS)
        if (includeRisk) add(AiSettingsArea.RISK)
        if (includeThresholds) add(AiSettingsArea.THRESHOLDS)
        if (includeScreener) add(AiSettingsArea.SCREENER)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "SUGGEST ALL SETTINGS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column {
                Text(
                    "Select the areas to configure and share your trading context.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeRss, onCheckedChange = { includeRss = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RSS FEEDS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeRisk, onCheckedChange = { includeRisk = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RISK PROFILE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeThresholds, onCheckedChange = { includeThresholds = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TECHNICAL THRESHOLDS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeScreener, onCheckedChange = { includeScreener = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCREENER MARKERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (lastPrompt.isNotBlank()) {
                    Text(
                        "PREVIOUS PROMPT",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        lastPrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Risk, style, regions, themes...") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(prompt, selectedAreas) },
                enabled = selectedAreas.isNotEmpty() && !isBusy
            ) {
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
private fun RssPreviewDialog(
    source: com.polaralias.signalsynthesis.data.rss.RssFeedSource,
    selectedTopicId: String?,
    previewStates: Map<String, RssPreviewState>,
    onSelectTopic: (String) -> Unit,
    onRequestPreview: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val topics = source.topics.filter { !it.isTickerTemplate }
    val selectedTopic = topics.firstOrNull { it.id == selectedTopicId } ?: topics.firstOrNull()
    val feedUrl = selectedTopic?.url

    LaunchedEffect(feedUrl) {
        if (feedUrl != null) {
            onRequestPreview(feedUrl)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "${source.label.uppercase()} PREVIEW",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column {
                if (topics.isEmpty()) {
                    Text(
                        "No topics available for preview.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(topics) { topic ->
                            FilterChip(
                                selected = topic.id == selectedTopic?.id,
                                onClick = { onSelectTopic(topic.id) },
                                label = { Text(topic.label.uppercase(), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val previewState = feedUrl?.let { previewStates[it] } ?: RssPreviewState()
                    when (previewState.status) {
                        RssPreviewStatus.LOADING -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = BrandSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Loading feed preview...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        RssPreviewStatus.ERROR -> {
                            Text(
                                previewState.errorMessage ?: "Preview unavailable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRed.copy(alpha = 0.7f)
                            )
                        }
                        RssPreviewStatus.READY -> {
                            if (previewState.items.isEmpty()) {
                                Text(
                                    "No recent items.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 320.dp)
                                ) {
                                    items(previewState.items) { item ->
                                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                            Text(
                                                item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                formatRssTimestamp(item.publishedAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            if (item.snippet.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    item.snippet,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        androidx.compose.material3.HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                        RssPreviewStatus.IDLE -> {
                            Text(
                                "Select a topic to preview.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", fontWeight = FontWeight.Black)
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
private fun RssToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = BrandPrimary,
                letterSpacing = 1.sp
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BrandPrimary,
                checkedTrackColor = BrandPrimary.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SettingsAccordionHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = BrandPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = BrandPrimary
            )
        }
    }
}

@Composable
private fun AiLockedCard(
    locked: Boolean,
    lockMessage: String,
    onLockedClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (locked) 0.45f else 1f)
        ) {
            content()
        }
        if (locked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                    .clickable { onLockedClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = BrandSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lockMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSuggestionCard(
    title: String,
    suggestionText: String,
    rationale: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    applyLabel: String = "APPLY",
    dismissLabel: String = "DISCARD"
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
                        Text(applyLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BrandSecondary)
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(dismissLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }
}

private fun formatRssSuggestionSummary(
    suggestion: AiRssSuggestion,
    catalog: com.polaralias.signalsynthesis.data.rss.RssFeedCatalog?
): String {
    val topicLabels = suggestion.enabledTopicKeys.mapNotNull { key ->
        catalog?.entryByKey(key)?.let { "${it.sourceLabel}: ${it.topicLabel}" }
    }
    val topicsText = if (topicLabels.isNotEmpty()) {
        val preview = topicLabels.take(4).joinToString(", ")
        val extra = if (topicLabels.size > 4) " +${topicLabels.size - 4} more" else ""
        preview + extra
    } else {
        "${suggestion.enabledTopicKeys.size} topics"
    }
    val tickerText = if (suggestion.tickerSourceIds.isEmpty()) {
        "None"
    } else {
        suggestion.tickerSourceIds.joinToString(", ") { id ->
            id.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }
    return "TOPICS: $topicsText | TICKER SOURCES: $tickerText"
}

private fun formatRssTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0L) return "--"
    val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private fun formatModelName(model: LlmModel): String {
    val suffix = if (model.lowCost) " (Low cost)" else ""
    return model.label + suffix
}

private fun groupedModelsForProvider(provider: LlmProvider): List<Pair<String, List<LlmModel>>> {
    val models = LlmModel.modelsForProvider(provider)
    if (models.isEmpty()) return emptyList()

    val reasoning = models.filter { it.visibilityGroup == LlmModelVisibilityGroup.CORE_REASONING }
    val execution = models.filter { it.visibilityGroup == LlmModelVisibilityGroup.EXECUTION_AUTOMATION }
    val additional = models.filter { it.visibilityGroup == LlmModelVisibilityGroup.ADDITIONAL }

    val groups = mutableListOf<Pair<String, List<LlmModel>>>()
    if (reasoning.isNotEmpty()) groups += "Reasoning & Synthesis" to reasoning
    if (execution.isNotEmpty()) groups += "Execution-Focused Research" to execution
    if (additional.isNotEmpty()) groups += "Additional" to additional
    return groups
}

private fun formatStageModelLabel(provider: LlmProvider, modelId: String): String {
    val normalized = LlmModel.normalizeModelIdAlias(modelId)
    val model = LlmModel.modelsForProvider(provider).firstOrNull { candidate ->
        candidate.modelId.equals(normalized, ignoreCase = true)
    } ?: LlmModel.fromModelId(normalized)
    return model?.let { formatModelName(it) } ?: modelId
}

private fun stageDisplayName(stage: AnalysisStage): String {
    return when (stage) {
        AnalysisStage.RSS_VERIFY -> "RSS REVIEW"
        AnalysisStage.SHORTLIST -> "CANDIDATE SHORTLIST"
        AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS -> "FUNDAMENTALS + NEWS SYNTHESIS"
        AnalysisStage.DECISION_UPDATE -> "FINAL THESIS UPDATE"
        AnalysisStage.DEEP_DIVE -> "DEEP ANALYSIS PASS"
    }
}

private fun stageDescription(stage: AnalysisStage): String {
    return when (stage) {
        AnalysisStage.RSS_VERIFY -> "Validate feed quality before pulling full content."
        AnalysisStage.SHORTLIST -> "Initial ranking pass over potential tickers."
        AnalysisStage.FUNDAMENTALS_NEWS_SYNTHESIS -> "Structured synthesis from fundamentals and catalysts."
        AnalysisStage.DECISION_UPDATE -> "Final conviction and keep/drop decisions."
        AnalysisStage.DEEP_DIVE -> "On-demand research pass with optional live web sources."
    }
}

private fun formatReasoningDepthLabel(
    depth: ReasoningDepth,
    provider: LlmProvider,
    modelId: String
): String {
    val normalizedModel = LlmModel.normalizeModelIdAlias(modelId).lowercase()
    val isGemini3 = provider == LlmProvider.GEMINI && normalizedModel.startsWith("gemini-3")
    val isGemini3Flash = isGemini3 && normalizedModel.contains("flash")
    val nativeControl = LlmModel.supportsNativeReasoningControl(provider, modelId)

    if (!nativeControl) {
        return when (depth) {
            ReasoningDepth.NONE -> "None"
            ReasoningDepth.MINIMAL -> "Minimal"
            ReasoningDepth.LOW -> "Low"
            ReasoningDepth.MEDIUM -> "Medium"
            ReasoningDepth.HIGH -> "High"
            ReasoningDepth.EXTRA -> "Maximum"
        }
    }

    if (provider == LlmProvider.GEMINI) {
        return when (depth) {
            ReasoningDepth.NONE, ReasoningDepth.MINIMAL -> if (isGemini3Flash) "Minimal" else "Low"
            ReasoningDepth.LOW -> "Low"
            ReasoningDepth.MEDIUM -> if (isGemini3Flash) "Medium" else "High"
            ReasoningDepth.HIGH, ReasoningDepth.EXTRA -> "High"
        }
    }

    return when (depth) {
        ReasoningDepth.NONE -> "None"
        ReasoningDepth.MINIMAL -> "Minimal"
        ReasoningDepth.LOW -> "Low"
        ReasoningDepth.MEDIUM -> "Medium"
        ReasoningDepth.HIGH -> "High"
        ReasoningDepth.EXTRA -> "High+"
    }
}

private fun formatCategoryName(category: com.polaralias.signalsynthesis.util.ApiUsageCategory): String {
    return when (category) {
        com.polaralias.signalsynthesis.util.ApiUsageCategory.DISCOVERY -> "Discovery"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.ANALYSIS -> "Synthesis"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.FUNDAMENTALS -> "Fundamentals"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.ALERTS -> "Monitoring"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.DEEP_DIVE -> "Deep Dive"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.SEARCH -> "Network Search"
        com.polaralias.signalsynthesis.util.ApiUsageCategory.OTHER -> "Protocols"
    }
}

