package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MilkEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DailyMilkBarChart(
    entries: List<MilkEntry>,
    modifier: Modifier = Modifier
) {
    // Group active records of the last 7 days of collection
    val sortedRecent = entries
        .groupBy { it.dateString }
        .mapValues { (_, entryList) -> entryList.sumOf { it.quantity } }
        .toList()
        .sortedBy { it.first }
        .takeLast(7)

    val maxVal = if (sortedRecent.isNotEmpty()) {
        sortedRecent.maxOf { it.second }.coerceAtLeast(10.0)
    } else {
        10.0
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Daily Milk Volume (Last 7 Active Days)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (sortedRecent.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No records logged yet. Add data to view chart.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                sortedRecent.forEach { (dateStr, qty) ->
                    val ratio = (qty / maxVal).toFloat().coerceIn(0.05f, 1f)
                    
                    // Simple Day labels: "15 Jun" style
                    val displayLabel = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                        SimpleDateFormat("dd MMM", Locale.getDefault()).format(parsed ?: Date())
                    } catch (e: Exception) {
                        dateStr.takeLast(5)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Numeric tooltip on top of bar
                        Text(
                            text = String.format(Locale.getDefault(), "%.1fL", qty),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(110.dp)
                        ) {
                            val barHeight = size.height * ratio
                            val barWidth = size.width
                            val xPos = 0f
                            val yPos = size.height - barHeight

                            // Rounded bar drawn with premium dual-color gradient
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(primaryColor, secondaryColor)
                                ),
                                topLeft = Offset(xPos, yPos),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = displayLabel,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyEarningsLineChart(
    entries: List<MilkEntry>,
    modifier: Modifier = Modifier
) {
    // Collect last 10 days of earnings for a smooth line trend progress mapping
    val sortedRevenue = entries
        .groupBy { it.dateString }
        .mapValues { (_, entryList) -> entryList.sumOf { it.totalAmount } }
        .toList()
        .sortedBy { it.first }
        .takeLast(10)

    val maxVal = if (sortedRevenue.isNotEmpty()) {
        sortedRevenue.maxOf { it.second }.coerceAtLeast(100.0)
    } else {
        100.0
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Earning Trend (Last 10 Days)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (sortedRevenue.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add at least 2 distinct days of records to show trends.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            val primaryColor = MaterialTheme.colorScheme.secondary
            val gradientFill = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val width = size.width
                val height = size.height
                val stepX = width / (sortedRevenue.size - 1)

                val points = sortedRevenue.mapIndexed { idx, (_, amt) ->
                    val ratio = (amt / maxVal).toFloat().coerceIn(0.1f, 0.9f)
                    Offset(idx * stepX, height - (height * ratio))
                }

                // Create curves Path
                val strokePath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val pPrev = points[i - 1]
                            val pCurr = points[i]
                            // Bezier curve control points
                            val conX1 = pPrev.x + (pCurr.x - pPrev.x) / 2
                            val conY1 = pPrev.y
                            val conX2 = pPrev.x + (pCurr.x - pPrev.x) / 2
                            val conY2 = pCurr.y

                            cubicTo(conX1, conY1, conX2, conY2, pCurr.x, pCurr.y)
                        }
                    }
                }

                // Area Fill path
                val fillPath = Path().apply {
                    addPath(strokePath)
                    if (points.isNotEmpty()) {
                        lineTo(points.last().x, height)
                        lineTo(points.first().x, height)
                        close()
                    }
                }

                // Draw Area Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientFill, Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw Stroke
                drawPath(
                    path = strokePath,
                    color = primaryColor,
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )

                // Circles on vertices
                points.forEach { point ->
                    drawCircle(
                        color = primaryColor,
                        radius = 6f,
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = point
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Draw dates label row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                sortedRevenue.forEachIndexed { i, (date, amt) ->
                    // Only show first, middle, and last date labels to prevent overlap
                    if (i == 0 || i == sortedRevenue.size / 2 || i == sortedRevenue.size - 1) {
                        val displayLabel = try {
                            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                            SimpleDateFormat("dd MMM", Locale.getDefault()).format(parsed ?: Date())
                        } catch (e: Exception) {
                            date.takeLast(5)
                        }

                        Text(
                            text = "$displayLabel (₹${amt.toInt()})",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
