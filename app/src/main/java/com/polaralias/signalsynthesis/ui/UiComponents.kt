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

fun formatPrice(value: Double): String = "$" + String.format("%.2f", value)

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

fun formatIntent(intent: TradingIntent): String = when (intent) {
    TradingIntent.DAY_TRADE -> "Day Trade"
    TradingIntent.SWING -> "Swing"
    TradingIntent.LONG_TERM -> "Long Term"
}
