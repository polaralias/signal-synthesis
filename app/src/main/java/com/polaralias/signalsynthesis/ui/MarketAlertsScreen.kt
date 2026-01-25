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
import com.polaralias.signalsynthesis.domain.model.*
import com.polaralias.signalsynthesis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketAlertsScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onRemoveAlert: (String) -> Unit,
    onAddToBlocklist: (String) -> Unit
) {
    var symbolToBlock by remember { mutableStateOf<String?>(null) }

    AmbientBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        RainbowMcpText(
                            text = "MARKET ALERTS", 
                            style = MaterialTheme.typography.titleLarge
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = RainbowBlue
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = RainbowBlue
                    ),
                    windowInsets = WindowInsets.systemBars
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
        if (uiState.alertSymbols.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "NO ACTIVE SIGNALS", 
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    letterSpacing = 2.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.alertSymbols) { symbol ->
                    AlertItem(
                        symbol = symbol,
                        onOpen = { onOpenDetail(symbol) },
                        onRemove = { onRemoveAlert(symbol) },
                        onBlock = { symbolToBlock = symbol }
                    )
                }
            }
        }

        if (symbolToBlock != null) {
            ConfirmBlocklistDialog(
                symbol = symbolToBlock!!,
                onConfirm = {
                    onAddToBlocklist(symbolToBlock!!)
                    symbolToBlock = null
                },
                onDismiss = { symbolToBlock = null }
            )
        }
    }
}
}

@Composable
private fun AlertItem(
    symbol: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit
) {
    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    symbol, 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Black,
                    color = RainbowBlue
                )
                Text(
                    "ACTIVE TELEMETRY MONITORING", 
                    style = MaterialTheme.typography.labelSmall,
                    color = RainbowPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 1.sp
                )
            }
            
            IconButton(onClick = onOpen) {
                Icon(Icons.Default.Info, contentDescription = "Analysis Info", tint = RainbowBlue)
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Exclude Temporarily", tint = MaterialTheme.colorScheme.outline)
            }
            
            IconButton(onClick = onBlock) {
                Icon(Icons.Default.Block, contentDescription = "Add to Blocklist", tint = RainbowRed)
            }
        }
    }
}

@Composable
fun ConfirmBlocklistDialog(
    symbol: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Blocklist $symbol?") },
        text = { Text("Are you sure you want to blocklist $symbol? It will be excluded from all future analysis and alerts until you remove it from the blocklist in settings.") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Blocklist")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
