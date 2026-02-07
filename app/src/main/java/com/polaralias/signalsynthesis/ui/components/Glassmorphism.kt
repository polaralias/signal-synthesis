package com.polaralias.signalsynthesis.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polaralias.signalsynthesis.ui.theme.*

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 12.dp,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgColor = if (isDark) {
        listOf(DarkSurface.copy(alpha = 0.7f), DarkSurface.copy(alpha = 0.5f))
    } else {
        listOf(Color.White.copy(alpha = 0.94f), LightSurface.copy(alpha = 0.88f))
    }
    
    val borderColor = if (isDark) GlassWhiteBorder else Color(0x3318273A)
    val sheenColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        // Blur Layer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(blurRadius)
                    .background(
                        Brush.verticalGradient(colors = bgColor)
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(DarkSurface.copy(alpha = 0.9f), DarkSurface.copy(alpha = 0.8f))
                            } else {
                                listOf(Color.White.copy(alpha = 0.98f), LightSurface.copy(alpha = 0.93f))
                            }
                        )
                    )
            )
        }

        // Sheen Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            sheenColor,
                            Color.Transparent,
                            sheenColor.copy(alpha = 0.1f)
                        )
                    )
                )
        )

        // Content Layer
        Box(modifier = Modifier.padding(1.dp)) {
            content()
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            shape = RoundedCornerShape(cornerRadius),
            modifier = modifier
        ) {
            GlassBox(cornerRadius = cornerRadius) {
                content()
            }
        }
    } else {
        GlassBox(modifier = modifier, cornerRadius = cornerRadius) {
            content()
        }
    }
}

