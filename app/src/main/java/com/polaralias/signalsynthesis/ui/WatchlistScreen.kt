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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polaralias.signalsynthesis.domain.model.*
import com.polaralias.signalsynthesis.ui.theme.*

@Composable
fun WatchlistScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onOpenSymbol: (String) -> Unit,
    onRemove: (String) -> Unit,
    onBlock: (String) -> Unit
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
                            text = "WATCHLIST",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Black)
                        )

                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.Transparent)
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {


                if (uiState.watchlist.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ViewInAr, 
                                contentDescription = null, 
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                "WATCHLIST EMPTY", 
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Run analysis to identify targets for monitoring.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.watchlist) { symbol ->
                            val setup = uiState.result?.setups?.find { it.symbol == symbol }
                                ?: uiState.history.flatMap { it.setups }.find { it.symbol == symbol }
                            
                            WatchlistItem(
                                symbol = symbol,
                                intent = setup?.intent,
                                source = setup?.source ?: com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED,
                                onClick = { onOpenSymbol(symbol) },
                                onRemove = { onRemove(symbol) },
                                onBlock = { symbolToBlock = symbol }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(48.dp)) }
                    }
                }
            }
        }
        if (symbolToBlock != null) {
            ConfirmBlocklistDialog(
                symbol = symbolToBlock!!,
                onConfirm = {
                    onBlock(symbolToBlock!!)
                    symbolToBlock = null
                },
                onDismiss = { symbolToBlock = null }
            )
        }
    }
}

@Composable
private fun WatchlistItem(
    symbol: String,
    intent: com.polaralias.signalsynthesis.domain.model.TradingIntent?,
    source: com.polaralias.signalsynthesis.domain.model.TickerSource,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit
) {
    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    symbol, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = BrandPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                SourceBadge(source)
                intent?.let {
                    Spacer(modifier = Modifier.width(12.dp))
                    IntentBadge(it)
                }
            }
            
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }
            
            IconButton(onClick = onBlock, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Block",
                    modifier = Modifier.size(18.dp),
                    tint = ErrorRed.copy(alpha = 0.4f)
                )
            }
        }
    }
}
