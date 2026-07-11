package com.smartlighting.app.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartlighting.app.data.model.AlarmRecord
import com.smartlighting.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDetailScreen(
    alarmId: Long,
    onBack: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var alarm by remember { mutableStateOf<AlarmRecord?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(alarmId) {
        isLoading = true
        val found = state.alarms.find { it.id == alarmId }
        alarm = found
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).background(Brush.verticalGradient(listOf(DarkBlue, Navy)))) {
        TopAppBar(
            title = { Text("告警详情", fontSize = 18.sp) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary)
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan)
            }
        } else if (alarm == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("告警不存在", color = TextMuted)
            }
        } else {
            val a = alarm!!
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column(Modifier.padding(16.dp)) {
                        val lvlColor = when (a.level) { "CRITICAL" -> Red; "WARNING" -> Amber; else -> PrimaryBlue }
                        Text(a.type.ifBlank { "告警" }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        DetailRow("设备", a.deviceId)
                        DetailRow("等级", a.level, lvlColor)
                        DetailRow("状态", a.status)
                        DetailRow("时间", a.startAt?.take(16) ?: "-")
                        DetailRow("原因", a.reason ?: "-")
                        DetailRow("处理人", a.handler ?: "-")
                    }
                }

                if (a.status == "ACTIVE") {
                    Button(
                        onClick = { viewModel.handleAlarm(a.id); onBack() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("确认处理", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: ", fontSize = 13.sp, color = TextMuted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
