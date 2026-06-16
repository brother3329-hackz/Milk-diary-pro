package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MilkViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: MilkViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsState()
    val entries by viewModel.allEntries.collectAsState()
    val scrollState = rememberScrollState()

    // Calculate dynamic period-over-period comparison metrics safely
    val (milkDiff, earningDiff, percentMilkUp, percentEarningUp) = remember(entries) {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val thirtyDaysMillis = 30 * oneDayMillis

        val last30Days = entries.filter { now - it.timestamp <= thirtyDaysMillis }
        val prev30Days = entries.filter {
            val age = now - it.timestamp
            age in (thirtyDaysMillis + 1).. (60 * oneDayMillis)
        }

        val last30Milk = last30Days.sumOf { it.quantity }
        val last30EarnIn = last30Days.sumOf { it.totalAmount }

        val prev30Milk = prev30Days.sumOf { it.quantity }
        val prev30EarnIn = prev30Days.sumOf { it.totalAmount }

        val mDiff = last30Milk - prev30Milk
        val eDiff = last30EarnIn - prev30EarnIn

        val mPercent = if (prev30Milk > 0) (mDiff / prev30Milk) * 100 else 0.0
        val ePercent = if (prev30EarnIn > 0) (eDiff / prev30EarnIn) * 100 else 0.0

        Quadruple(mDiff, eDiff, mPercent, ePercent)
    }

    val avgRate = remember(entries) {
        if (entries.isNotEmpty()) {
            entries.sumOf { it.rate } / entries.size
        } else {
            0.0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Business Intelligence & Trends", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen tagline
            Text(
                text = "Advanced milk delivery insights and weekly trends for planning feed costs and rates.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Lifetime Milestones section
            Text(
                text = "Operational Milestones",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Best Earning Day Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Star",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Best Earning Day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("₹${String.format(Locale.getDefault(), "%.1f", stats.bestEarningAmount)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = try {
                                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stats.bestEarningDate)
                                SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(parsed ?: Date())
                            } catch (e: Exception) {
                                stats.bestEarningDate
                            },
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Highest single collection yield card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Drop",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Peak Yield Volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("${String.format(Locale.getDefault(), "%.1f L", stats.highestMilkQty)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = try {
                                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stats.highestMilkDate)
                                SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(parsed ?: Date())
                            } catch (e: Exception) {
                                stats.highestMilkDate
                            },
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Period over Period Comparison analytics card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "30-Day Period Comparison",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Comparing the past 30 days of transactions against the previous 30-day period.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Volume delta mapping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Milk Quantity Delta", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Volume contrast",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        val volumeTrendColor = if (milkDiff >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (milkDiff >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = volumeTrendColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%+.1f L (%+.1f%%)", milkDiff, percentMilkUp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = volumeTrendColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Business Earned cash mapping delta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Revenue Delta", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Gross income contrast",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        val cashTrendColor = if (earningDiff >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (earningDiff >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = cashTrendColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val earningSign = if (earningDiff >= 0) "+" else "-"
                            val absEarningDiff = kotlin.math.abs(earningDiff)
                            Text(
                                text = String.format(Locale.getDefault(), "%s₹%.0f (%+.1f%%)", earningSign, absEarningDiff, percentEarningUp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = cashTrendColor
                            )
                        }
                    }
                }
            }

            // Key Business Efficiency Indicators
            Text(
                text = "Key General Metrics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Average Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("₹${String.format(Locale.getDefault(), "%.2f", avgRate)}", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Divider(modifier = Modifier.height(35.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Orders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${entries.size} Slips", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// Simple Container class
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
