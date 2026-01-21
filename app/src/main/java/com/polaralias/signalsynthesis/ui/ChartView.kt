package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun ChartView(
    points: List<PricePoint>,
    vwap: Double? = null,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No chart data available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val vwapColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val minPrice = points.minOf { it.price }
    val maxPrice = points.maxOf { it.price }
    val currentPrice = points.last().price
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                    )
                ),
                shape = MaterialTheme.shapes.medium
            )
            .padding(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 48.dp) // Space for Y axis labels on right
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val dataMin = minOf(minPrice, vwap ?: minPrice)
            val dataMax = maxOf(maxPrice, vwap ?: maxPrice)
            val priceRange = (dataMax - dataMin).coerceAtLeast(0.01)
            
            // Add some padding to the range
            val yMin = dataMin - (priceRange * 0.15)
            val yMax = dataMax + (priceRange * 0.15)
            val yRange = yMax - yMin

            val xStep = width / (points.size - 1).coerceAtLeast(1)

            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, point ->
                val x = index * xStep
                val y = height - ((point.price - yMin) / yRange * height).toFloat()

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == points.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.lineTo(0f, height)
                    fillPath.close()
                }
            }

            // Draw area fill with premium gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.4f),
                        primaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            )

            // Draw grid lines
            val gridColor = onSurface.copy(alpha = 0.05f)
            val gridCount = 4
            for (i in 0..gridCount) {
                val y = height * (i.toFloat() / gridCount)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw VWAP line if present
            vwap?.let { v ->
                val vwapY = height - ((v - yMin) / yRange * height).toFloat()
                drawLine(
                    color = vwapColor.copy(alpha = 0.6f),
                    start = Offset(0f, vwapY),
                    end = Offset(width, vwapY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw the price line
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // Draw latest price point
            val lastX = width
            val lastY = height - ((currentPrice - yMin) / yRange * height).toFloat()
            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = surfaceColor,
                radius = 2.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
        
        // Y-Axis Labels (Price)
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(start = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatPrice(maxPrice), style = MaterialTheme.typography.labelSmall, color = onSurface.copy(alpha = 0.6f))
            Text(formatPrice((maxPrice + minPrice) / 2), style = MaterialTheme.typography.labelSmall, color = onSurface.copy(alpha = 0.6f))
            Text(formatPrice(minPrice), style = MaterialTheme.typography.labelSmall, color = onSurface.copy(alpha = 0.6f))
        }

        // VWAP Tag if present
        vwap?.let {
            val relY = 1f - ((it - (minOf(minPrice, vwap) - ( (maxOf(maxPrice, vwap) - minOf(minPrice, vwap)).coerceAtLeast(0.01) * 0.15))) / ((maxOf(maxPrice, vwap) - minOf(minPrice, vwap)).coerceAtLeast(0.01) * 1.3))
            // Simplified positioning logic for the tag since we are in a Box
            // For now, let's just put a small badge at the top if VWAP is significant
        }
    }
}
