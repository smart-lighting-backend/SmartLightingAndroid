package com.smartlighting.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartlighting.app.ui.components.SimpleBarChart
import com.smartlighting.app.ui.components.SimpleLineChart
import com.smartlighting.app.ui.components.SimplePieChart
import com.smartlighting.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DarkBlue, Navy)))) {
        TopAppBar(
            title = { Text("数据报表", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary)
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Energy trend
                state.energyTrend?.let { trend ->
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("能耗报表 (kWh)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.height(12.dp))
                                SimpleLineChart(trend.current, trend.lastWeek, trend.labels, height = 240.dp)
                            }
                        }
                    }
                }

                // Device status pie
                state.healthSummary?.let { hs ->
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("设备状态分布", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                val pieData = listOf(
                                    "健康" to hs.healthyCount.toDouble(),
                                    "警告" to hs.warningCount.toDouble(),
                                    "严重" to hs.criticalCount.toDouble()
                                )
                                SimplePieChart(pieData, height = 200.dp)
                            }
                        }
                    }
                }

                // Alarm stats bar
                state.alarmStats?.let { stats ->
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("告警等级统计", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                val barData = stats.byLevel.map { (k, v) -> k to v.toDouble() }
                                SimpleBarChart(barData, height = 180.dp)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
