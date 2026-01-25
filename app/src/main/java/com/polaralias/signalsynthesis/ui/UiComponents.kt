package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
                            listOf(RainbowBlue, RainbowPurple)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            RainbowMcpText(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun AmbientBackground(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val moveX by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moveX"
    )
    val moveY by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moveY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RainbowBlue.copy(alpha = if (isDark) 0.15f else 0.05f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * 0.1f + moveX, 
                        size.height * 0.1f + moveY
                    ),
                    radius = size.width * 1.5f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RainbowRed.copy(alpha = if (isDark) 0.12f else 0.04f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * 0.9f - moveX, 
                        size.height * 0.9f - moveY
                    ),
                    radius = size.width * 1.2f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RainbowGreen.copy(alpha = if (isDark) 0.08f else 0.03f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * 0.5f + moveY, 
                        size.height * 0.5f + moveX
                    ),
                    radius = size.width * 1.0f
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
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            RainbowBlue,
            RainbowGreen,
            RainbowYellow,
            RainbowRed,
            RainbowBlue
        ),
        start = androidx.compose.ui.geometry.Offset(offset, 0f),
        end = androidx.compose.ui.geometry.Offset(offset + 1000f, 1000f),
        tileMode = TileMode.Repeated
    )

    Text(
        text = text,
        style = style.copy(
            brush = brush,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        ),
        modifier = modifier
    )
}

@Composable
fun IntentBadge(intent: TradingIntent) {
    val color = when (intent) {
        TradingIntent.DAY_TRADE -> RainbowBlue
        TradingIntent.SWING -> RainbowPurple
        TradingIntent.LONG_TERM -> RainbowGreen
        else -> RainbowBlue
    }
    val icon = when (intent) {
        TradingIntent.DAY_TRADE -> Icons.Filled.Timer
        TradingIntent.SWING -> Icons.Filled.ShowChart
        TradingIntent.LONG_TERM -> Icons.Filled.CalendarToday
        else -> Icons.Filled.Help
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatIntent(intent),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

@Composable
fun SourceBadge(source: com.polaralias.signalsynthesis.domain.model.TickerSource) {
    val color = when (source) {
        com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED -> MaterialTheme.colorScheme.onSurfaceVariant
        com.polaralias.signalsynthesis.domain.model.TickerSource.SCREENER -> RainbowPurple
        com.polaralias.signalsynthesis.domain.model.TickerSource.CUSTOM -> RainbowBlue
        com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_GAINER -> RainbowGreen
        com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_LOSER -> RainbowRed
        com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_ACTIVE -> RainbowOrange
    }

    val icon = when (source) {
        com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED -> Icons.Filled.Bookmarks
        com.polaralias.signalsynthesis.domain.model.TickerSource.SCREENER -> Icons.Filled.Search
        com.polaralias.signalsynthesis.domain.model.TickerSource.CUSTOM -> Icons.Filled.Edit
        com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_GAINER -> Icons.Filled.TrendingUp
        com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_LOSER -> Icons.Filled.TrendingDown
        com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_ACTIVE -> Icons.Filled.Bolt
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .border(0.5.dp, color.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatTickerSource(source),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
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
    com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_GAINER -> "Gainer"
    com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_LOSER -> "Loser"
    com.polaralias.signalsynthesis.domain.model.TickerSource.LIVE_ACTIVE -> "Active"
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
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = RainbowRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SYSTEM OFFLINE / MOCK MODE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = RainbowRed,
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
                fontWeight = FontWeight.Black,
                color = RainbowRed,
                fontSize = 10.sp
            )
        }
    }
}
