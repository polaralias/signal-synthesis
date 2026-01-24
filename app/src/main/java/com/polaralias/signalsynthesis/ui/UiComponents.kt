package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

fun formatPrice(value: Double): String {
    return if (value < 1.0 && value > 0) {
        "$" + String.format("%.5f", value)
    } else if (value < 10.0) {
        "$" + String.format("%.4f", value)
    } else {
        "$" + String.format("%.2f", value)
    }
}

fun formatPercent(value: Double): String = "${(value * 100.0).roundToInt()}%"

fun formatTime(instant: Instant?): String {
    if (instant == null) return "--"
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}

@Composable
fun SectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun IntentBadge(intent: TradingIntent) {
    androidx.compose.material3.Surface(
        color = when (intent) {
            TradingIntent.DAY_TRADE -> MaterialTheme.colorScheme.tertiaryContainer
            TradingIntent.SWING -> MaterialTheme.colorScheme.secondaryContainer
            TradingIntent.LONG_TERM -> MaterialTheme.colorScheme.primaryContainer
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        Text(
            text = formatIntent(intent),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = when (intent) {
                TradingIntent.DAY_TRADE -> MaterialTheme.colorScheme.onTertiaryContainer
                TradingIntent.SWING -> MaterialTheme.colorScheme.onSecondaryContainer
                TradingIntent.LONG_TERM -> MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}

@Composable
fun SourceBadge(source: com.polaralias.signalsynthesis.domain.model.TickerSource) {
    androidx.compose.material3.Surface(
        color = when (source) {
            com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED -> MaterialTheme.colorScheme.surfaceVariant
            com.polaralias.signalsynthesis.domain.model.TickerSource.SCREENER -> MaterialTheme.colorScheme.tertiaryContainer
            com.polaralias.signalsynthesis.domain.model.TickerSource.CUSTOM -> MaterialTheme.colorScheme.primaryContainer
            com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_GAINER -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
            com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_LOSER -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
            com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_ACTIVE -> MaterialTheme.colorScheme.secondaryContainer
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        Text(
            text = formatTickerSource(source),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = when (source) {
                com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED -> MaterialTheme.colorScheme.onSurfaceVariant
                com.polaralias.signalsynthesis.domain.model.TickerSource.SCREENER -> MaterialTheme.colorScheme.onTertiaryContainer
                com.polaralias.signalsynthesis.domain.model.TickerSource.CUSTOM -> MaterialTheme.colorScheme.onPrimaryContainer
                com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_GAINER -> androidx.compose.ui.graphics.Color.White
                com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_LOSER -> androidx.compose.ui.graphics.Color.White
                com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_ACTIVE -> MaterialTheme.colorScheme.onSecondaryContainer
            }
        )
    }
}

fun formatIntent(intent: TradingIntent): String = when (intent) {
    TradingIntent.DAY_TRADE -> "Day Trade"
    TradingIntent.SWING -> "Swing"
    TradingIntent.LONG_TERM -> "Long Term"
}

fun formatTickerSource(source: com.polaralias.signalsynthesis.domain.model.TickerSource): String = when (source) {
    com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED -> "Static"
    com.polaralias.signalsynthesis.domain.model.TickerSource.SCREENER -> "Screener"
    com.polaralias.signalsynthesis.domain.model.TickerSource.CUSTOM -> "Custom"
    com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_GAINER -> "📈 Gainer"
    com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_LOSER -> "📉 Loser"
    com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_ACTIVE -> "🔥 Active"
}

@Composable
fun MockModeBanner(isVisible: Boolean, onClick: () -> Unit = {}) {
    if (!isVisible) return
    
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        Column(modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = "⚠️ MOCK MODE ACTIVE",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = "Synthesis is running in mock mode only. Real market data providers are currently disabled because no API keys have been configured.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
