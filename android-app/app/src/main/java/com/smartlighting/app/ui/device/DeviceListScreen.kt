package com.smartlighting.app.ui.device

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartlighting.app.data.model.BatchOperationResult
import com.smartlighting.app.data.model.Device
import kotlinx.coroutines.delay
import com.smartlighting.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DeviceListScreen(
    onDeviceClick: (String) -> Unit,
    onAddDevice: () -> Unit,
    onEditDevice: (String) -> Unit,
    onBatchImport: () -> Unit = {},
    viewModel: DeviceViewModel = hiltViewModel()
) {
    val listState by viewModel.listState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var contextMenuDevice by remember { mutableStateOf<Device?>(null) }
    val lazyListState = rememberLazyListState()

    // Selection mode
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    // Batch action feedback
    var batchActionMsg by remember { mutableStateOf<String?>(null) }

    // Trigger initial load + auto-refresh every 10s while on this page
    LaunchedEffect(Unit) {
        if (listState.devices.isEmpty() && !listState.isLoading) {
            viewModel.loadDevices()
        }
        while (true) {
            delay(10_000)
            if (!listState.isLoading && !listState.isLoadingMore) {
                viewModel.refresh()
            }
        }
    }

    // Pagination: load more when near bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = lazyListState.layoutInfo.totalItemsCount
            listState.hasMore && !listState.isLoadingMore && lastVisible >= totalItems - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    // Batch action callback
    fun doBatchAction(action: String) {
        val ids = selectedIds.toList()
        if (ids.isEmpty()) return
        viewModel.batchControl(ids, action) { ok, result ->
            selectionMode = false
            selectedIds.clear()
            val label = when (action) {
                "ON" -> "开灯"
                "OFF" -> "关灯"
                "ENABLE" -> "启用"
                "DISABLE" -> "停用"
                else -> action
            }
            if (ok) {
                val s = result?.success ?: 0
                batchActionMsg = "批量$label 成功 $s 台"
            } else {
                batchActionMsg = "批量$label 失败"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DarkBlue, Navy)))) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        if (selectionMode) "已选 ${selectedIds.size} 台"
                        else "设备管理",
                        fontSize = if (selectionMode) 16.sp else 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary),
                actions = {
                    if (!selectionMode) {
                        IconButton(onClick = onBatchImport) {
                            Icon(Icons.Default.FileUpload, contentDescription = "批量导入", tint = Cyan)
                        }
                        IconButton(onClick = {
                            selectionMode = true
                        }) {
                            Icon(Icons.Default.CheckBox, contentDescription = "选择", tint = Cyan)
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = Cyan)
                        }
                    }
                }
            )

            if (listState.isLoading && listState.devices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Cyan)
                }
            } else if (listState.error != null && listState.devices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加载失败", fontSize = 16.sp, color = TextMuted)
                        Text(listState.error ?: "", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadDevices() }) {
                            Text("点击重试", color = Cyan)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp, top = 12.dp,
                        bottom = if (selectionMode) 72.dp else 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Area filter chips
                    if (listState.districts.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = listState.areaFilter == null,
                                    onClick = { viewModel.setAreaFilter(null) },
                                    label = { Text("全部", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = White
                                    )
                                )
                                listState.districts.forEach { d ->
                                    val selected = listState.areaFilter == d.name
                                    FilterChip(
                                        selected = selected,
                                        onClick = { viewModel.setAreaFilter(d.name) },
                                        label = { Text(d.name, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryBlue,
                                            selectedLabelColor = White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Search bar + add button (only when not in selection mode)
                    if (!selectionMode) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = listState.keyword,
                                    onValueChange = { viewModel.loadDevices(it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("搜索设备名称/编号", color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryBlue, unfocusedBorderColor = Color(0x33E2E8F0),
                                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                FilledTonalButton(onClick = onAddDevice, colors = ButtonDefaults.filledTonalButtonColors(containerColor = PrimaryBlue)) {
                                    Icon(Icons.Default.Add, null, tint = Color.White)
                                    Text("新增", color = Color.White)
                                }
                            }
                        }
                    }

                    // Device count
                    item {
                        Text(
                            "共 ${listState.total} 台设备",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Device grid — 4 per row
                    val rows = listState.devices.chunked(4)
                    items(rows.size) { rowIndex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rows[rowIndex].forEach { device ->
                                SelectableDeviceCard(
                                    device = device,
                                    modifier = Modifier.weight(1f),
                                    selectionMode = selectionMode,
                                    isSelected = device.id != null && selectedIds.contains(device.id),
                                    onClick = {
                                        if (selectionMode) {
                                            device.id?.let { id ->
                                                if (selectedIds.contains(id)) selectedIds.remove(id)
                                                else selectedIds.add(id)
                                            }
                                        } else {
                                            onDeviceClick(device.deviceId)
                                        }
                                    },
                                    onLongClick = {
                                        if (!selectionMode) contextMenuDevice = device
                                    }
                                )
                            }
                            // Fill remaining slots
                            repeat(4 - rows[rowIndex].size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Loading more
                    if (listState.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Cyan, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    if (!selectionMode) {
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // Bottom action bar for batch operations
        if (selectionMode && selectedIds.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = CardBg,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BatchActionButton("开灯", Green) { doBatchAction("ON") }
                    BatchActionButton("关灯", Amber) { doBatchAction("OFF") }
                    BatchActionButton("启用", Green) { doBatchAction("ENABLE") }
                    BatchActionButton("停用", Red) { doBatchAction("DISABLE") }
                }
            }
        }

        // Snackbar feedback
        batchActionMsg?.let { msg ->
            LaunchedEffect(msg) {
                delay(2000)
                batchActionMsg = null
            }
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = Navy,
                contentColor = TextPrimary
            ) {
                Text(msg, fontSize = 13.sp)
            }
        }
    }

    // Context menu dialog
    contextMenuDevice?.let { device ->
        AlertDialog(
            onDismissRequest = { contextMenuDevice = null },
            title = { Text(device.name?.ifBlank { device.deviceId } ?: device.deviceId, color = TextPrimary) },
            text = { Text("设备编号: ${device.deviceId}", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onEditDevice(device.deviceId)
                    contextMenuDevice = null
                }) { Text("编辑", color = Cyan) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = device.deviceId
                    contextMenuDevice = null
                }) { Text("删除", color = Red) }
            }
        )
    }

    showDeleteDialog?.let { devId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除设备") },
            text = { Text("确认删除设备 $devId ？") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteDevice(devId); showDeleteDialog = null }) { Text("删除", color = Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun BatchActionButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableDeviceCard(
    device: Device,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val statusColor = when (device.status) { 1 -> Green; 2 -> TextMuted; 3 -> Amber; else -> TextMuted }
    val statusLabel = when (device.status) { 1 -> "在线"; 2 -> "离线"; 3 -> "异常"; else -> "停用" }
    val lightLabel = when (device.lightOn) { true -> "开灯"; false -> "关灯"; null -> "—" }
    val lightColor = when (device.lightOn) { true -> Amber; false -> TextSecondary; null -> TextMuted }

    val borderMod = if (selectionMode && isSelected) {
        Modifier.border(1.5.dp, Cyan, RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .then(borderMod)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else CardBg)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectionMode) {
                Icon(
                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    null,
                    tint = if (isSelected) Cyan else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.height(2.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(3.dp))
                Text(statusLabel, fontSize = 9.sp, color = statusColor)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                device.name?.ifBlank { device.deviceId } ?: device.deviceId,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                device.deviceId,
                fontSize = 9.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(lightLabel, fontSize = 10.sp, color = lightColor)
        }
    }
}
