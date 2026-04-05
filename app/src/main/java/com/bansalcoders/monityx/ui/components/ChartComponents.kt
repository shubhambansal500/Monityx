package com.bansalcoders.monityx.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bansalcoders.monityx.domain.model.CategorySpend
import com.bansalcoders.monityx.domain.model.MonthlySpend
import com.bansalcoders.monityx.ui.theme.ChartColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Donut / Pie Chart
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Animated donut chart for category spend breakdown.
 * Pure Compose Canvas implementation – no external chart library required.
 */
@Composable
fun DonutChart(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier,
    strokeWidthDp: Float = 48f,
) {
    if (data.isEmpty()) return

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900, easing = EaseInOutCubic),
        label = "donut_anim",
    )

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val strokePx  = strokeWidthDp.dp.toPx()
            val diameter  = min(size.width, size.height) - strokePx
            val topLeft   = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize   = Size(diameter, diameter)
            var startAngle = -90f

            data.forEachIndexed { index, slice ->
                val sweep = slice.percentage / 100f * 360f * animProgress
                val color = ChartColors[index % ChartColors.size]

                drawArc(
                    color       = color,
                    startAngle  = startAngle,
                    sweepAngle  = sweep,
                    useCenter   = false,
                    topLeft     = topLeft,
                    size        = arcSize,
                    style       = Stroke(width = strokePx, cap = StrokeCap.Butt),
                )
                startAngle += slice.percentage / 100f * 360f
            }
        }

        // Legend
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            data.take(6).forEachIndexed { index, slice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = ChartColors[index % ChartColors.size])
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text     = "${slice.category.label} (${slice.percentage.toInt()}%)",
                        style    = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Line Chart
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Animated line chart for monthly spending trend.
 * Pure Compose Canvas implementation.
 */
@Composable
fun SpendingLineChart(
    data: List<MonthlySpend>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900, easing = EaseInOutCubic),
        label = "line_anim",
    )

    val primaryColor  = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 8.dp)
        ) {
            val paddingLeft   = 56f
            val paddingBottom = 40f
            val paddingTop    = 16f
            val paddingRight  = 16f

            val chartWidth  = size.width  - paddingLeft - paddingRight
            val chartHeight = size.height - paddingBottom - paddingTop

            val maxValue = data.maxOf { it.amount }.coerceAtLeast(1.0)
            val minValue = 0.0

            // Draw grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = paddingTop + chartHeight - (chartHeight * i / gridLines)
                drawLine(
                    color = onSurface,
                    start = Offset(paddingLeft, y),
                    end   = Offset(paddingLeft + chartWidth, y),
                    strokeWidth = 1f,
                )
            }

            if (data.size < 2) return@Canvas

            // Build path points
            val points = data.mapIndexed { index, item ->
                val x = paddingLeft + (index.toFloat() / (data.size - 1)) * chartWidth
                val y = paddingTop + chartHeight - ((item.amount - minValue) / (maxValue - minValue)).toFloat() * chartHeight
                Offset(x, y)
            }

            val animatedPoints = if (animProgress < 1f) {
                val endIndex = (points.size * animProgress).toInt().coerceAtMost(points.size - 1)
                points.take(endIndex + 1)
            } else points

            // Gradient fill under the line
            if (animatedPoints.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(animatedPoints.first().x, paddingTop + chartHeight)
                    animatedPoints.forEach { lineTo(it.x, it.y) }
                    lineTo(animatedPoints.last().x, paddingTop + chartHeight)
                    close()
                }
                drawPath(
                    path  = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.3f),
                            primaryColor.copy(alpha = 0f),
                        ),
                        startY = paddingTop,
                        endY   = paddingTop + chartHeight,
                    ),
                )

                // Draw line
                val linePath = Path().apply {
                    moveTo(animatedPoints.first().x, animatedPoints.first().y)
                    animatedPoints.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path  = linePath,
                    color = primaryColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                // Dots at each data point
                animatedPoints.forEach { pt ->
                    drawCircle(color = primaryColor, radius = 4f, center = pt)
                    drawCircle(color = Color.White,  radius = 2f, center = pt)
                }
            }
        }

        // X-axis labels (show every other label if too many)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 64.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val step = if (data.size > 6) 2 else 1
            data.filterIndexed { i, _ -> i % step == 0 }.forEach { item ->
                Text(
                    text  = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
