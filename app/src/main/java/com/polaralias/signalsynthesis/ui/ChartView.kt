package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polaralias.signalsynthesis.ui.theme.*

@Composable
fun ChartView(
    points: List<PricePoint>,
    vwap: Double? = null,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        com.polaralias.signalsynthesis.ui.components.GlassBox(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "CHART DATA SUSPENDED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
        return
    }

    val primaryColor = BrandPrimary
    val secondaryColor = BrandSecondary
    val vwapColor = WarningOrange
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val minPrice = points.minOf { it.price }
    val maxPrice = points.maxOf { it.price }
    val currentPrice = points.last().price
    
    com.polaralias.signalsynthesis.ui.components.GlassBox(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "REAL-TIME SPECTRAL ANALYSIS", 
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    formatPrice(currentPrice),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.weight(1f).padding(end = 56.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    val dataMin = minOf(minPrice, vwap ?: minPrice)
                    val dataMax = maxOf(maxPrice, vwap ?: maxPrice)
                    val priceRange = (dataMax - dataMin).coerceAtLeast(0.0001)
                    
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

                    // Draw area fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.2f),
                                primaryColor.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw grid lines
                    val gridColor = onSurface.copy(alpha = 0.05f)
                    val gridCount = 4
                    for (i in 0..gridCount) {
                        val y = (height * (i.toFloat() / gridCount)).toFloat()
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
                            color = vwapColor.copy(alpha = 0.4f),
                            start = Offset(0f, vwapY),
                            end = Offset(width, vwapY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    }

                    // Draw the glow (shadow)
                    drawPath(
                        path = path,
                        color = primaryColor.copy(alpha = 0.3f),
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )

                    // Draw the main price line
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )

                    // Draw latest price point with pulse effect (simulated)
                    val lastX = width
                    val lastY = height - ((currentPrice - yMin) / yRange * height).toFloat()
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.2f),
                        radius = 8.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                }
                
                // Y-Axis Labels
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().offset(x = 56.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    LabelText(formatPrice(maxPrice))
                    LabelText(formatPrice((maxPrice + minPrice) / 2))
                    LabelText(formatPrice(minPrice))
                }
            }
        }
    }
}

@Composable
private fun LabelText(text: String) {
    Text(
        text = text, 
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        fontWeight = FontWeight.Black
    )
}

