package com.smartlighting.app.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartlighting.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DarkBlue, Navy)))) {
        TopAppBar(
            title = { Text("智能助手", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary)
        )

        // Device selector + diagnose button
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device dropdown
                Box(modifier = Modifier.weight(1f)) {
                    val selectedDevice = state.devices.find { it.deviceId == state.selectedDeviceId }
                    val label = if (selectedDevice != null) "${selectedDevice.deviceId} ${selectedDevice.name?.ifBlank { null } ?: ""}".trim()
                    else "选择设备"

                    OutlinedButton(
                        onClick = { viewModel.showDevicePicker(!state.showDevicePicker) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = state.showDevicePicker,
                        onDismissRequest = { viewModel.showDevicePicker(false) },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        state.devices.take(30).forEach { device ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(device.deviceId, fontSize = 13.sp, color = TextPrimary)
                                        if (!device.name.isNullOrBlank()) {
                                            Text(device.name, fontSize = 11.sp, color = TextMuted)
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectDevice(device.deviceId)
                                    viewModel.showDevicePicker(false)
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Diagnose button
                Button(
                    onClick = { viewModel.runDiagnose() },
                    enabled = state.selectedDeviceId.isNotBlank() && !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("一键诊断", fontSize = 12.sp, color = Cyan)
                }
            }
        }

        // Chat messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) PrimaryBlue else CardBg
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            msg.content.stripMarkdown(),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = if (msg.isUser) Color.White else TextPrimary
                        )
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Cyan, strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = viewModel::onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue, unfocusedBorderColor = Color(0x33E2E8F0),
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.sendMessage() },
                enabled = !state.isLoading && state.inputText.isNotBlank()
            ) {
                Icon(Icons.Default.Send, "发送", tint = if (state.inputText.isNotBlank()) Cyan else TextMuted)
            }
        }
    }
}

/** 去除常见 Markdown 标记，使 AI 回复在纯文本中更美观 */
private fun String.stripMarkdown(): String {
    return this
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^-\\s+", RegexOption.MULTILINE), "· ")
        .replace(Regex("`{1,3}(.+?)`{1,3}"), "$1")
        .replace(Regex("~~(.+?)~~"), "$1")
        .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        .trim()
}
