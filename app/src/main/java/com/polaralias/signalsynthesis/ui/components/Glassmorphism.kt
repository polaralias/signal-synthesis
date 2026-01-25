package com.polaralias.signalsynthesis.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polaralias.signalsynthesis.ui.theme.GlassBackground
import com.polaralias.signalsynthesis.ui.theme.GlassBorder

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 8.dp, // Reduced default blur
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        // Blur Layer (Only for background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(blurRadius)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E202C).copy(alpha = 0.7f),
                                Color(0xFF1E202C).copy(alpha = 0.5f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E202C).copy(alpha = 0.9f),
                                Color(0xFF1E202C).copy(alpha = 0.8f)
                            )
                        )
                    )
            )
        }

        // Overlay to give it that "glass" sheen
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.01f)
                        )
                    )
                )
        )

        // Content Layer (Un-blurred)
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
