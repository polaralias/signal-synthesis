package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import com.polaralias.signalsynthesis.ui.theme.*
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
            .padding(top = 24.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(4.dp, 16.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun AmbientBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonBlue.copy(alpha = 0.08f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.2f),
                    radius = size.width * 0.8f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPurple.copy(alpha = 0.08f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.7f),
                    radius = size.width * 0.9f
                )
            )
        }
        content()
    }
}

@Composable
fun RainbowMcpText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            NeonBlue,
            NeonPurple,
            NeonGreen,
            NeonBlue
        ),
        start = androidx.compose.ui.geometry.Offset(offset, 0f),
        end = androidx.compose.ui.geometry.Offset(offset + 300f, 300f),
        tileMode = TileMode.Mirror
    )

    Text(
        text = text,
        style = style.copy(brush = brush),
        modifier = modifier
    )
}

@Composable
fun IntentBadge(intent: TradingIntent) {
    val color = when (intent) {
        TradingIntent.DAY_TRADE -> com.polaralias.signalsynthesis.ui.theme.NeonBlue
        TradingIntent.SWING -> com.polaralias.signalsynthesis.ui.theme.NeonPurple
        TradingIntent.LONG_TERM -> com.polaralias.signalsynthesis.ui.theme.NeonGreen
        else -> com.polaralias.signalsynthesis.ui.theme.NeonBlue
    }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = formatIntent(intent),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
fun SourceBadge(source: com.polaralias.signalsynthesis.domain.model.TickerSource) {
    val color = when (source) {
        com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED -> androidx.compose.ui.graphics.Color.Gray
        com.polaralias.signalsynthesis.domain.model.TickerSource.SCREENER -> com.polaralias.signalsynthesis.ui.theme.NeonPurple
        com.polaralias.signalsynthesis.domain.model.TickerSource.CUSTOM -> com.polaralias.signalsynthesis.ui.theme.NeonBlue
        com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_GAINER -> com.polaralias.signalsynthesis.ui.theme.NeonGreen
        com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_LOSER -> com.polaralias.signalsynthesis.ui.theme.NeonRed
        com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_ACTIVE -> com.polaralias.signalsynthesis.ui.theme.NeonOrange
    }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .border(0.5.dp, color.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = formatTickerSource(source),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

fun formatIntent(intent: TradingIntent): String = when (intent) {
    TradingIntent.DAY_TRADE -> "Day Trade"
    TradingIntent.SWING -> "Swing"
    TradingIntent.LONG_TERM -> "Long Term"
    else -> "Unknown"
}

fun formatTickerSource(source: com.polaralias.signalsynthesis.domain.model.TickerSource): String = when (source) {
    com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED -> "Static"
    com.polaralias.signalsynthesis.domain.model.TickerSource.SCREENER -> "Screener"
    com.polaralias.signalsynthesis.domain.model.TickerSource.CUSTOM -> "Custom"
    com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_GAINER -> "📈 Gainer"
    com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_LOSER -> "📉 Loser"
    com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_ACTIVE -> "🔥 Active"
}

fun formatLargeNumber(value: Long): String {
    return when {
        value >= 1_000_000_000 -> String.format("%.1fB", value / 1_000_000_000.0)
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}

@Composable
fun MockModeBanner(isVisible: Boolean, onClick: () -> Unit = {}) {
    if (!isVisible) return
    
    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚠️ SYSTEM OFFLINE / MOCK MODE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = com.polaralias.signalsynthesis.ui.theme.NeonRed,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Synthesis is running in simulation mode. Real-time market protocols are disabled until valid API credentials are provided.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "TAP TO CONFIGURE AUTHENTICATION KEYS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = com.polaralias.signalsynthesis.ui.theme.NeonRed,
                fontSize = 10.sp
            )
        }
    }
}
