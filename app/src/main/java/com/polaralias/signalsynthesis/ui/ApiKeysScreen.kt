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
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.clickable
import com.polaralias.signalsynthesis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onFieldChanged: (KeyField, String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AmbientBackground {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            text = "AUTHENTICATION",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                        )

                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color.Transparent)
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {


                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (uiState.blacklistedProviders.isNotEmpty()) {
                        com.polaralias.signalsynthesis.ui.components.GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Rainbow4, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "PROTOCOL SUSPENDED",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Rainbow4,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        "Credentials rejected by some nodes. Automatic retry in progress.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        "MARKET DATA ENCRYPTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                    
                    ApiKeyField(
                        value = uiState.keys.alpacaKey,
                        onValueChange = { onFieldChanged(KeyField.ALPACA_KEY, it) },
                        label = "Alpaca Key ID",
                        isBlacklisted = uiState.blacklistedProviders.contains("Alpaca")
                    )
                    
                    ApiKeyField(
                        value = uiState.keys.alpacaSecret,
                        onValueChange = { onFieldChanged(KeyField.ALPACA_SECRET, it) },
                        label = "Alpaca Secret Key",
                        isBlacklisted = false
                    )

                    ApiKeyField(
                        value = uiState.keys.massiveKey,
                        onValueChange = { onFieldChanged(KeyField.MASSIVE, it) },
                        label = "Polygon API Key",
                        isBlacklisted = uiState.blacklistedProviders.contains("Massive")
                    )

                    ApiKeyField(
                        value = uiState.keys.finnhubKey,
                        onValueChange = { onFieldChanged(KeyField.FINNHUB, it) },
                        label = "Finnhub Access",
                        isBlacklisted = uiState.blacklistedProviders.contains("Finnhub")
                    )

                    ApiKeyField(
                        value = uiState.keys.fmpKey,
                        onValueChange = { onFieldChanged(KeyField.FMP, it) },
                        label = "FMP Access",
                        isBlacklisted = uiState.blacklistedProviders.contains("Fmp")
                    )

                    ApiKeyField(
                        value = uiState.keys.twelveDataKey,
                        onValueChange = { onFieldChanged(KeyField.TWELVE_DATA, it) },
                        label = "Twelve Data Access",
                        isBlacklisted = uiState.blacklistedProviders.contains("TwelveData")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "AI SERVICE CONFIGURATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandSecondary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    ApiKeyField(
                        value = uiState.keys.anthropicKey,
                        onValueChange = { onFieldChanged(KeyField.ANTHROPIC, it) },
                        label = "Anthropic API Key",
                        isBlacklisted = false
                    )

                    ApiKeyField(
                        value = uiState.keys.openAiKey,
                        onValueChange = { onFieldChanged(KeyField.OPENAI, it) },
                        label = "OpenAI API Key",
                        isBlacklisted = false
                    )

                    ApiKeyField(
                        value = uiState.keys.geminiKey,
                        onValueChange = { onFieldChanged(KeyField.GEMINI, it) },
                        label = "Gemini API Key",
                        isBlacklisted = false
                    )

                    ApiKeyField(
                        value = uiState.keys.minimaxKey,
                        onValueChange = { onFieldChanged(KeyField.MINIMAX, it) },
                        label = "MiniMax API Key",
                        isBlacklisted = false
                    )

                    ApiKeyField(
                        value = uiState.keys.openRouterKey,
                        onValueChange = { onFieldChanged(KeyField.OPENROUTER, it) },
                        label = "OpenRouter API Key",
                        isBlacklisted = false
                    )

                    ApiKeyField(
                        value = uiState.keys.togetherKey,
                        onValueChange = { onFieldChanged(KeyField.TOGETHER, it) },
                        label = "Together API Key",
                        isBlacklisted = false
                    )

                    ApiKeyField(
                        value = uiState.keys.groqKey,
                        onValueChange = { onFieldChanged(KeyField.GROQ, it) },
                        label = "Groq API Key",
                        isBlacklisted = false
                    )

                    ApiKeyField(
                        value = uiState.keys.deepseekKey,
                        onValueChange = { onFieldChanged(KeyField.DEEPSEEK, it) },
                        label = "DeepSeek API Key",
                        isBlacklisted = false
                    )

                    ApiKeyField(
                        value = uiState.keys.siliconFlowKey,
                        onValueChange = { onFieldChanged(KeyField.SILICONFLOW, it) },
                        label = "SiliconFlow API Key",
                        isBlacklisted = false
                    )

                    ApiKeyField(
                        value = uiState.keys.customLlmKey,
                        onValueChange = { onFieldChanged(KeyField.CUSTOM_LLM, it) },
                        label = "Custom LLM API Key (Optional)",
                        isBlacklisted = false
                    )

                    Text(
                        text = "Local providers (Ollama, LocalAI, vLLM, TGI, SGLang) do not require API keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    com.polaralias.signalsynthesis.ui.components.GlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable { 
                                onSave()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Security protocols updated. Encrypted storage synchronized.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "COMMIT CONFIGURATION",
                                color = BrandPrimary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    com.polaralias.signalsynthesis.ui.components.GlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { 
                                onClear()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Credentials cleared. All keys removed.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "CLEAR ALL KEYS",
                                color = ErrorRed.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            label = { 
                Text(
                    label.uppercase(), 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ) 
            },
            isError = isBlacklisted,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = if (isBlacklisted) ErrorRed else BrandPrimary,
                unfocusedBorderColor = if (isBlacklisted) ErrorRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
            ),
            trailingIcon = if (isBlacklisted) {
                { Icon(Icons.Filled.Block, contentDescription = "Blocked", tint = ErrorRed, modifier = Modifier.padding(end = 8.dp)) }
            } else null,
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                autoCorrect = false
            )
        )
        if (isBlacklisted) {
            Text(
                "ACCESS DENIED: INVALID CREDENTIALS",
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                letterSpacing = 1.sp
            )
        }
    }
}

