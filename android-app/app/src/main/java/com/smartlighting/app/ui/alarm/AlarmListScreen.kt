package com.smartlighting.app.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartlighting.app.ui.components.SimpleBarChart
import com.smartlighting.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAlarmClick: (Long) -> Unit = {},
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // 10s silent auto-refresh
    LaunchedEffect(Unit) {
        while (true) { delay(10_000); viewModel.refresh() }
    }

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DarkBlue, Navy)))) {
        TopAppBar(
            title = { Text("告警中心", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary),
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = Cyan)
                }
            }
        )

        if (state.isLoading && state.alarms.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stats
                state.stats?.let { stats ->
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatItem("活跃告警", stats.totalActive.toString(), Red)
                                StatItem("紧急", stats.byLevel["CRITICAL"]?.toString() ?: "0", Red)
                                StatItem("严重", stats.byLevel["MAJOR"]?.toString() ?: "0", Amber)
                                StatItem("警告", stats.byLevel["WARNING"]?.toString() ?: "0", PrimaryBlue)
                            }
                        }
                    }
                }

                // Trend
                if (state.trend.isNotEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("7日告警趋势", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.height(8.dp))
                                val barData = state.trend.map { it.date.takeLast(5) to it.count.toDouble() }
                                SimpleBarChart(barData, height = 140.dp)
                            }
                        }
                    }
                }

                // Level filter
                item {
                    Text("告警级别", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(null to "全部", "CRITICAL" to "紧急", "MAJOR" to "严重", "WARNING" to "警告").forEach { (key, label) ->
                            val color = when (key) { "CRITICAL" -> Red; "MAJOR" -> Amber; "WARNING" -> PrimaryBlue; else -> PrimaryBlue }
                            FilterChip(
                                selected = state.levelFilter == key,
                                onClick = { viewModel.setLevelFilter(key) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (key != null) color else PrimaryBlue,
                                    selectedLabelColor = White
                                )
                            )
                        }
                    }
                }

                // Status filter
                item {
                    Text("告警状态", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(null to "全部", "ACTIVE" to "活跃", "ACKNOWLEDGED" to "已确认", "RECOVERED" to "已解决").forEach { (key, label) ->
                            FilterChip(
                                selected = state.statusFilter == key,
                                onClick = { viewModel.setStatusFilter(key) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue, selectedLabelColor = White
                                )
                            )
                        }
                    }
                }

                // Type filter
                item {
                    Text("告警类型", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(null to "全部", "OFFLINE" to "离线", "FAULT" to "故障", "HEALTH_LOW" to "健康分过低").forEach { (key, label) ->
                            FilterChip(
                                selected = state.typeFilter == key,
                                onClick = { viewModel.setTypeFilter(key) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue, selectedLabelColor = White
                                )
                            )
                        }
                    }
                }

                // Date filter
                item {
                    Text("时间范围", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.startTime ?: "",
                            onValueChange = { viewModel.setDateRange(it.ifBlank { null }, state.endTime) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("开始时间", fontSize = 11.sp, color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue, unfocusedBorderColor = Color(0x33E2E8F0),
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        Text("—", color = TextMuted, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
                        OutlinedTextField(
                            value = state.endTime ?: "",
                            onValueChange = { viewModel.setDateRange(state.startTime, it.ifBlank { null }) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("结束时间", fontSize = 11.sp, color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue, unfocusedBorderColor = Color(0x33E2E8F0),
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                // Total count + page size selector
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("共 ${state.total} 条告警", fontSize = 12.sp, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(5, 10, 15).forEach { size ->
                                FilterChip(
                                    selected = state.pageSize == size,
                                    onClick = { viewModel.setPageSize(size) },
                                    label = { Text("$size 条", fontSize = 10.sp) },
                                    modifier = Modifier.height(24.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue, selectedLabelColor = White
                                    )
                                )
                            }
                        }
                    }
                }

                // Alarm cards
                items(state.alarms.size, key = { state.alarms[it].id }) { idx ->
                    val alarm = state.alarms[idx]
                    val lvlLabel = when (alarm.level) { "CRITICAL" -> "紧急"; "MAJOR" -> "严重"; "WARNING" -> "警告"; else -> alarm.level }
                    val lvlColor = when (alarm.level) { "CRITICAL" -> Red; "MAJOR" -> Amber; "WARNING" -> PrimaryBlue; else -> TextMuted }
                    val stLabel = when (alarm.status) { "ACTIVE" -> "活跃"; "ACKNOWLEDGED" -> "已确认"; "RECOVERED" -> "已解决"; else -> alarm.status }
                    val stColor = when (alarm.status) { "ACTIVE" -> Red; "ACKNOWLEDGED" -> Cyan; "RECOVERED" -> Green; else -> TextMuted }

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onAlarmClick(alarm.id) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(alarm.deviceId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(lvlLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = lvlColor)
                                    Text(" · ", fontSize = 11.sp, color = TextMuted)
                                    Text(stLabel, fontSize = 11.sp, color = stColor)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(alarm.type.ifBlank { "—" }, fontSize = 11.sp, color = TextSecondary)
                                Row {
                                    if (alarm.status == "ACTIVE") {
                                        TextButton(
                                            onClick = { viewModel.handleAlarm(alarm.id) },
                                            modifier = Modifier.height(24.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) { Text("确认处理", fontSize = 11.sp, color = Cyan) }
                                    }
                                    Text(alarm.startAt?.replace("T", " ")?.take(16) ?: "-", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                            if (alarm.reason != null) {
                                Text(alarm.reason, fontSize = 10.sp, color = TextMuted, maxLines = 1)
                            }
                        }
                    }
                }

                // Page navigation
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.goToPage(state.currentPage - 1) },
                            enabled = state.currentPage > 1
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, "上一页", tint = if (state.currentPage > 1) Cyan else TextMuted)
                        }
                        Text(
                            "第 ${state.currentPage} / ${state.totalPages} 页",
                            fontSize = 13.sp, color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { viewModel.goToPage(state.currentPage + 1) },
                            enabled = state.currentPage < state.totalPages
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, "下一页", tint = if (state.currentPage < state.totalPages) Cyan else TextMuted)
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}
