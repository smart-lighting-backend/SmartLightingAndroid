package com.smartlighting.app.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import com.smartlighting.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventScreen(viewModel: EventViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    // 10s auto-refresh
    LaunchedEffect(state.activeTab) {
        while (true) { delay(10_000); viewModel.silentRefresh() }
    }

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DarkBlue, Navy)))) {
        TopAppBar(
            title = { Text("事件中心", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary),
            actions = {
                IconButton(onClick = { if (state.activeTab == 0) viewModel.loadVision() else viewModel.loadVoice() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = Cyan)
                }
            }
        )

        TabRow(selectedTabIndex = state.activeTab, containerColor = DarkBlue, contentColor = Cyan) {
            Tab(selected = state.activeTab == 0, onClick = { viewModel.loadVision() }) {
                Text("视觉事件", modifier = Modifier.padding(12.dp), fontSize = 14.sp)
            }
            Tab(selected = state.activeTab == 1, onClick = { viewModel.loadVoice() }) {
                Text("语音事件", modifier = Modifier.padding(12.dp), fontSize = 14.sp)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Device ID search
                item {
                    OutlinedTextField(
                        value = state.deviceIdFilter,
                        onValueChange = { viewModel.setDeviceIdFilter(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索设备编号", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue, unfocusedBorderColor = Color(0x33E2E8F0),
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (state.activeTab == 0) {
                    val page = state.visionPage; val totalPages = state.visionTotalPages; val total = state.visionTotal

                    // Vision type filter
                    item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(null to "全部", "行人检测" to "行人检测", "车辆通行" to "车辆通行", "异常停车" to "异常停车", "危险场景" to "危险场景", "策略联动拍照" to "策略联动拍照").forEach { (key, label) ->
                                FilterChip(
                                    selected = state.visionTypeFilter == key,
                                    onClick = { viewModel.setVisionTypeFilter(key) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue, selectedLabelColor = White)
                                )
                            }
                        }
                    }

                    // Total + page size
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("共 $total 条视觉事件", fontSize = 12.sp, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(5, 10, 15).forEach { size ->
                                    FilterChip(
                                        selected = state.pageSize == size,
                                        onClick = { viewModel.setPageSize(size) },
                                        label = { Text("$size 条", fontSize = 10.sp) },
                                        modifier = Modifier.height(24.dp),
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue, selectedLabelColor = White)
                                    )
                                }
                            }
                        }
                    }

                    items(state.visionEvents.size, key = { state.visionEvents[it].id ?: it }) { idx ->
                        val e = state.visionEvents[idx]
                        val typeColor = when (e.eventType) { "行人检测" -> Green; "车辆通行" -> Cyan; "异常停车" -> Amber; "危险场景" -> Red; else -> PrimaryBlue }
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(e.deviceId, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(e.eventType.ifBlank { "视觉事件" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = typeColor)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("置信度: ${String.format("%.1f", e.confidence * 100)}%", fontSize = 12.sp, color = TextSecondary)
                                Text(e.occurredAt?.replace("T", " ")?.take(19) ?: "-", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }

                    // Page navigation — vision
                    item {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.goToVisionPage(page - 1) }, enabled = page > 1) {
                                Icon(Icons.Default.KeyboardArrowLeft, "上一页", tint = if (page > 1) Cyan else TextMuted)
                            }
                            Text("第 $page / $totalPages 页", fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { viewModel.goToVisionPage(page + 1) }, enabled = page < totalPages) {
                                Icon(Icons.Default.KeyboardArrowRight, "下一页", tint = if (page < totalPages) Cyan else TextMuted)
                            }
                        }
                    }
                } else {
                    val page = state.voicePage; val totalPages = state.voiceTotalPages; val total = state.voiceTotal

                    // Voice type filter
                    item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(null to "全部", "播报" to "播报", "广播" to "广播", "警告" to "警告").forEach { (key, label) ->
                                FilterChip(
                                    selected = state.voiceTypeFilter == key,
                                    onClick = { viewModel.setVoiceTypeFilter(key) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue, selectedLabelColor = White)
                                )
                            }
                        }
                    }
                    // Voice source filter
                    item {
                        Text("来源", fontSize = 11.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(null to "全部", "自动" to "自动", "策略联动" to "策略联动").forEach { (key, label) ->
                                FilterChip(
                                    selected = state.voiceSourceFilter == key,
                                    onClick = { viewModel.setVoiceSourceFilter(key) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue, selectedLabelColor = White)
                                )
                            }
                        }
                    }

                    // Total + page size
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("共 $total 条语音事件", fontSize = 12.sp, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(5, 10, 15).forEach { size ->
                                    FilterChip(
                                        selected = state.pageSize == size,
                                        onClick = { viewModel.setPageSize(size) },
                                        label = { Text("$size 条", fontSize = 10.sp) },
                                        modifier = Modifier.height(24.dp),
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue, selectedLabelColor = White)
                                    )
                                }
                            }
                        }
                    }

                    items(state.voiceEvents.size, key = { state.voiceEvents[it].id ?: it }) { idx ->
                        val e = state.voiceEvents[idx]
                        val typeColor = when (e.type) { "警告" -> Red; "播报" -> Green; "广播" -> Cyan; else -> PrimaryBlue }
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(e.deviceId, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(e.type.ifBlank { "语音事件" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = typeColor)
                                }
                                if (e.content != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(e.content, fontSize = 12.sp, color = TextSecondary, maxLines = 3)
                                }
                                Spacer(Modifier.height(2.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("来源: ${e.source ?: "—"}", fontSize = 11.sp, color = TextSecondary)
                                    Text(e.occurredAt?.replace("T", " ")?.take(19) ?: "-", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    }

                    // Page navigation — voice
                    item {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.goToVoicePage(page - 1) }, enabled = page > 1) {
                                Icon(Icons.Default.KeyboardArrowLeft, "上一页", tint = if (page > 1) Cyan else TextMuted)
                            }
                            Text("第 $page / $totalPages 页", fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { viewModel.goToVoicePage(page + 1) }, enabled = page < totalPages) {
                                Icon(Icons.Default.KeyboardArrowRight, "下一页", tint = if (page < totalPages) Cyan else TextMuted)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
