package com.smartlighting.app.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartlighting.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: String,
    onBack: () -> Unit,
    viewModel: DeviceViewModel = hiltViewModel()
) {
    val state by viewModel.detailState.collectAsState()

    // Initial load (with spinner) + 5s silent auto-refresh
    LaunchedEffect(deviceId) {
        viewModel.loadDeviceDetail(deviceId)
        while (true) {
            delay(5_000)
            viewModel.loadDeviceDetail(deviceId, silent = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).background(Brush.verticalGradient(listOf(DarkBlue, Navy)))) {
        TopAppBar(
            title = { Text(state.device?.name ?: "设备详情", fontSize = 18.sp) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary)
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan)
            }
        } else {
            val d = state.device ?: return
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Info + Running Status card ──
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                        Column(Modifier.padding(16.dp)) {
                            DetailRow("编号", d.deviceId)
                            DetailRow("区域", d.area?.ifBlank { "-" } ?: "-")
                            DetailRow("位置", d.location?.ifBlank { "-" } ?: "-")
                            val st = when (d.status) { 1 -> "在线"; 2 -> "离线"; 3 -> "异常"; else -> "停用" }
                            val stC = when (d.status) { 1 -> Green; 2 -> TextMuted; 3 -> Amber; else -> TextMuted }
                            DetailRow("设备状态", st, stC)

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = DarkBlue)
                            Spacer(Modifier.height(8.dp))

                            // Light running status
                            val lightLabel = when (d.lightOn) { true -> "已开灯"; false -> "已关灯"; null -> "未知" }
                            val lightColor = when (d.lightOn) { true -> Amber; false -> TextSecondary; null -> TextMuted }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("运行状态: ", fontSize = 13.sp, color = TextMuted)
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(lightColor))
                                Spacer(Modifier.width(6.dp))
                                Text(lightLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = lightColor)
                            }
                            val bri = d.brightness
                            if (bri != null) {
                                Spacer(Modifier.height(6.dp))
                                Text("当前亮度: ", fontSize = 13.sp, color = TextMuted)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { bri / 100f },
                                        modifier = Modifier.weight(1f).height(8.dp),
                                        color = if (bri >= 70) Amber else if (bri >= 40) Cyan else TextSecondary,
                                        trackColor = DarkBlue,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("$bri%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cyan)
                                }
                            }
                            val src = d.controlSource
                            if (src != null) {
                                Spacer(Modifier.height(4.dp))
                                Text("控制方式: ${if (src == "MANUAL") "手动" else "自动"}", fontSize = 12.sp, color = TextSecondary)
                            }
                            val hc = if (d.healthScore >= 80) Green else if (d.healthScore >= 60) Amber else Red
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = DarkBlue)
                            Spacer(Modifier.height(8.dp))
                            DetailRow("健康分", String.format("%.0f", d.healthScore), hc)
                        }
                    }
                }

                // Health card
                state.health?.let { h ->
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("健康评估", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.height(8.dp))
                                DetailRow("总体评分", String.format("%.0f", h.overallScore), Cyan)
                                DetailRow("等级", h.level)
                                if (h.suggestion.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(h.suggestion, fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                // Telemetry
                state.telemetry?.data?.let { tel ->
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("最新遥测", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TelemetryCell("光照", tel.illuminance?.let { String.format("%.0f", it) } ?: "-", "lux")
                                    TelemetryCell("温度", tel.temperature?.let { String.format("%.1f", it) } ?: "-", "℃")
                                    TelemetryCell("湿度", tel.humidity?.let { String.format("%.1f", it) } ?: "-", "%")
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TelemetryCell("PM2.5", tel.pm25?.let { String.format("%.1f", it) } ?: "-", "μg/m³")
                                    TelemetryCell("AQI", tel.aqi?.let { String.format("%.1f", it) } ?: "-", "")
                                    TelemetryCell("人体", if (tel.pir == 1) "有人" else "无人", "")
                                }
                            }
                        }
                    }
                }

                // Perception panel
                state.perception?.let { perc ->
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("融合感知", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.height(8.dp))
                                @Suppress("UNCHECKED_CAST")
                                val tel = perc["telemetry"] as? Map<String, Any?>
                                if (tel != null) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TelemetryCell("光照", tel["illuminance"]?.toString() ?: "-", "lux")
                                        TelemetryCell("温度", tel["temperature"]?.toString() ?: "-", "℃")
                                        TelemetryCell("车流", tel["trafficFlow"]?.toString() ?: "-", "辆")
                                    }
                                }
                                @Suppress("UNCHECKED_CAST")
                                val vision = perc["latestVision"] as? Map<String, Any?>
                                val voice = perc["latestVoice"] as? Map<String, Any?>
                                if (vision != null || voice != null) {
                                    Spacer(Modifier.height(4.dp))
                                    if (vision != null) {
                                        DetailRow("视觉事件", "${vision["eventType"] ?: "-"} (置信度: ${vision["confidence"] ?: "-"})")
                                    }
                                    if (voice != null) {
                                        DetailRow("语音事件", "${voice["type"] ?: "-"}: ${voice["content"] ?: "-"}")
                                    }
                                }
                            }
                        }
                    }
                }

                // Control panel
                item {
                    val currentBri = d.brightness ?: 0
                    var sliderBri by remember(d.deviceId) { mutableFloatStateOf(currentBri.toFloat()) }
                    val isOff = d.lightOn == false

                    // Keep slider in sync when brightness changes from backend
                    LaunchedEffect(currentBri) {
                        sliderBri = currentBri.toFloat()
                    }

                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("控制面板", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(Modifier.height(8.dp))

                            // ON/OFF buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.controlDevice(deviceId, "ON") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.weight(1f)
                                ) { Text("开灯") }
                                Button(onClick = { viewModel.controlDevice(deviceId, "OFF") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.weight(1f)
                                ) { Text("关灯") }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Restart + Unlock buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.controlDevice(deviceId, "RESTART") },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber),
                                    modifier = Modifier.weight(1f)
                                ) { Text("重启") }
                                if (d.controlSource == "MANUAL" || d.manualMode == true) {
                                    OutlinedButton(onClick = { viewModel.unlockDevice(deviceId) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("恢复自动") }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Brightness slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("亮度", fontSize = 13.sp, color = TextSecondary)
                                Spacer(Modifier.width(8.dp))
                                Slider(
                                    value = sliderBri,
                                    onValueChange = { sliderBri = it },
                                    onValueChangeFinished = {
                                        viewModel.controlDevice(deviceId, "DIMMING", sliderBri.toInt())
                                    },
                                    enabled = !isOff,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Cyan,
                                        activeTrackColor = Cyan,
                                        inactiveTrackColor = DarkBlue,
                                        disabledThumbColor = TextMuted,
                                        disabledActiveTrackColor = TextMuted.copy(alpha = 0.3f)
                                    ),
                                    valueRange = 0f..100f
                                )
                                Text(
                                    "${sliderBri.toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOff) TextMuted else Cyan,
                                    modifier = Modifier.width(40.dp)
                                )
                            }
                        }
                    }
                }

                // Control history
                if (state.controlHistory.isNotEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("控制历史", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.height(8.dp))
                                state.controlHistory.take(10).forEachIndexed { index, h ->
                                    if (index > 0) {
                                        Spacer(Modifier.height(6.dp))
                                        HorizontalDivider(color = DarkBlue)
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    val actionLabel = when (h.action) {
                                        "ON" -> "开灯"
                                        "OFF" -> "关灯"
                                        "DIMMING" -> "调光"
                                        "RESTART" -> "重启"
                                        else -> h.action
                                    }
                                    val actionColor = when (h.action) {
                                        "ON" -> Green; "OFF" -> Red; "DIMMING" -> Cyan; "RESTART" -> Amber; else -> TextPrimary
                                    }
                                    val statusLabel = when (h.status) { "SENT" -> "已发送"; "ACKED" -> "已确认"; "FAILED" -> "失败"; else -> h.status ?: "-" }
                                    val statusColor = when (h.status) { "SENT" -> Cyan; "ACKED" -> Green; "FAILED" -> Red; else -> TextMuted }

                                    Row(verticalAlignment = Alignment.Top) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(actionColor))
                                                Spacer(Modifier.width(6.dp))
                                                Text(actionLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = actionColor)
                                                if (h.brightness != null) {
                                                    Text(" · ${h.brightness}%", fontSize = 12.sp, color = Cyan)
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text("操作: ${h.operator ?: "—"}", fontSize = 11.sp, color = TextSecondary)
                                            if (h.issuedAt != null) {
                                                Text(h.issuedAt.replace("T", " ").take(19), fontSize = 11.sp, color = TextMuted)
                                            }
                                        }
                                        Text(statusLabel, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                                    }
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
fun TelemetryCell(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Cyan)
        Text("$label ($unit)", fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("$label: ", fontSize = 13.sp, color = TextMuted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
