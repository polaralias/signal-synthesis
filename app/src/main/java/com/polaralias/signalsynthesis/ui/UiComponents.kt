package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
