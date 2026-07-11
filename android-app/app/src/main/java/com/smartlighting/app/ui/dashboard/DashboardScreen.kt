package com.smartlighting.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import com.smartlighting.app.ui.components.AmapView
import com.smartlighting.app.ui.components.SimpleLineChart
import com.smartlighting.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onDeviceClick: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBlue, Navy)))
    ) {
        TopAppBar(
            title = { Text("数字孪生概览", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary)
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stats grid
                item {
                    val s = uiState.stats
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard("设备总数", s?.totalDevices?.toString() ?: "--", "在线 ${s?.onlineDevices ?: "--"}", Green, Modifier.weight(1f).fillMaxHeight())
                        StatCard("在线率", "${s?.onlineRate ?: "--"}%", "设备运行状态", Cyan, Modifier.weight(1f).fillMaxHeight())
                        StatCard("未处理告警", s?.alertCount?.toString() ?: "--", "点击告警查看", Red, Modifier.weight(1f).fillMaxHeight())
                    }
                }

                // Device map
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("设备分布地图", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                if (uiState.selectedArea.isNotBlank()) {
                                    TextButton(onClick = { viewModel.selectArea(uiState.selectedArea) }) {
                                        Text("清除筛选", color = Cyan, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Area filter chips
                            val areas = uiState.mapDevices.map { it.area ?: "" }.filter { it.isNotBlank() }.distinct()
                            if (areas.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                var chipScrollState = rememberScrollState()
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(chipScrollState),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    areas.forEach { area ->
                                        val selected = uiState.selectedArea == area
                                        FilterChip(
                                            selected = selected,
                                            onClick = { viewModel.selectArea(area) },
                                            label = { Text(area, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryBlue,
                                                selectedLabelColor = androidx.compose.ui.graphics.Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            if (uiState.mapDevices.isNotEmpty()) {
                                AmapView(
                                    devices = uiState.mapDevices,
                                    selectedArea = uiState.selectedArea,
                                    modifier = Modifier.fillMaxWidth().height(360.dp),
                                    onDeviceClick = { deviceId -> onDeviceClick(deviceId) }
                                )
                            } else {
                                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("暂无设备位置数据", color = TextMuted)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, hint: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = TextMuted)
            Text(hint, fontSize = 11.sp, color = TextSecondary)
        }
    }
}
