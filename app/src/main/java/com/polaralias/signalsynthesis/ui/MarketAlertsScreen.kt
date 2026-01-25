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
                            text = "MONITORING",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                        )

                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = Color.Transparent)
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                AppHeader(
                    title = "ALERTS",
                    subtitle = "Active neural monitoring nodes"
                )

                if (uiState.alertSymbols.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Radar, 
                                contentDescription = null, 
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "NO ACTIVE SIGNALS", 
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    color = BrandPrimary
                )
                Text(
                    "LIVE TELEMETRY MONITORING", 
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = BrandSecondary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            
            IconButton(onClick = onOpen) {
                Icon(Icons.Default.Analytics, contentDescription = "Analysis Detail", tint = BrandPrimary, modifier = Modifier.size(20.dp))
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.VisibilityOff, contentDescription = "Remove Temporarily", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
            }
            
            IconButton(onClick = onBlock) {
                Icon(Icons.Default.Block, contentDescription = "Blacklist Node", tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
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
        title = { 
            Text(
                "BLACKLIST NODE: $symbol", 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ) 
        },
        text = { 
            Text(
                "Exclude $symbol from all future protocols and autonomous monitoring? This action is recorded in the blocklist.",
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("BLACKLIST", color = ErrorRed, fontWeight = FontWeight.Black)
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

