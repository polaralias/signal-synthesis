package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onFieldChanged: (KeyField, String) -> Unit,
    onSave: () -> Unit
) {
    AmbientBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        RainbowMcpText(
                            text = "NODE AUTHENTICATION", 
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.blacklistedProviders.isNotEmpty()) {
                com.polaralias.signalsynthesis.ui.components.GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "AUTH PROTOCOL FAILURE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = com.polaralias.signalsynthesis.ui.theme.NeonRed,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "One or more market nodes are temporarily suspended due to invalid credentials. Session will reset in 10 minutes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Text(
                "ESTABLISH SECURE LINK TO MARKET DECODERS:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            ApiKeyField(
                value = uiState.keys.alpacaKey,
                onValueChange = { onFieldChanged(KeyField.ALPACA_KEY, it) },
                label = "Alpaca API Key",
                isBlacklisted = uiState.blacklistedProviders.contains("Alpaca")
            )
            
            ApiKeyField(
                value = uiState.keys.alpacaSecret,
                onValueChange = { onFieldChanged(KeyField.ALPACA_SECRET, it) },
                label = "Alpaca Secret",
                isBlacklisted = false
            )

            ApiKeyField(
                value = uiState.keys.massiveKey,
                onValueChange = { onFieldChanged(KeyField.MASSIVE, it) },
                label = "Massive API Key",
                isBlacklisted = uiState.blacklistedProviders.contains("Massive")
            )

            ApiKeyField(
                value = uiState.keys.finnhubKey,
                onValueChange = { onFieldChanged(KeyField.FINNHUB, it) },
                label = "Finnhub API Key",
                isBlacklisted = uiState.blacklistedProviders.contains("Finnhub")
            )

            ApiKeyField(
                value = uiState.keys.fmpKey,
                onValueChange = { onFieldChanged(KeyField.FMP, it) },
                label = "FMP API Key",
                isBlacklisted = uiState.blacklistedProviders.contains("Fmp")
            )

            ApiKeyField(
                value = uiState.keys.twelveDataKey,
                onValueChange = { onFieldChanged(KeyField.TWELVE_DATA, it) },
                label = "Twelve Data API Key",
                isBlacklisted = uiState.blacklistedProviders.contains("TwelveData")
            )

            SectionHeader("AI COGNITIVE HUB")
            val llmLabel = when (uiState.appSettings.llmProvider) {
                LlmProvider.OPENAI -> "OpenAI Signature"
                LlmProvider.GEMINI -> "Gemini Signature"
            }
            ApiKeyField(
                value = uiState.keys.llmKey,
                onValueChange = { onFieldChanged(KeyField.LLM, it) },
                label = llmLabel,
                isBlacklisted = false
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .background(com.polaralias.signalsynthesis.ui.theme.NeonGreen.copy(alpha = 0.2f))
                    .border(1.dp, com.polaralias.signalsynthesis.ui.theme.NeonGreen.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .clickable { onSave() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "COMMIT CONFIGURATION",
                    color = com.polaralias.signalsynthesis.ui.theme.NeonGreen,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isBlacklisted: Boolean
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isBlacklisted,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = if (isBlacklisted) {
                { Text("🚫", modifier = Modifier.padding(end = 8.dp)) }
            } else null,
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                autoCorrect = false
            )
        )
        if (isBlacklisted) {
            Text(
                "Provider temporarily paused due to authentication errors.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
