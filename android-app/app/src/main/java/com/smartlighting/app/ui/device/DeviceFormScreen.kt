package com.smartlighting.app.ui.device

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
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
import com.amap.api.maps.model.LatLng
import com.smartlighting.app.ui.components.AmapView
import com.smartlighting.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceFormScreen(
    deviceId: String = "",
    onBack: () -> Unit,
    viewModel: DeviceViewModel = hiltViewModel()
) {
    val isEditing = deviceId.isNotBlank()
    var devId by remember { mutableStateOf(deviceId) }
    var name by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var factorySerial by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(!isEditing) }

    // Map picker state
    var showMapPicker by remember { mutableStateOf(false) }
    var pendingLatLng by remember { mutableStateOf<LatLng?>(null) }
    var mapSelectedArea by remember { mutableStateOf("") }
    val mapState by viewModel.mapPickerState.collectAsState()

    // Load existing data for editing
    val detailState by viewModel.detailState.collectAsState()
    LaunchedEffect(deviceId) {
        if (isEditing) viewModel.loadDeviceDetail(deviceId)
    }
    LaunchedEffect(detailState.device) {
        if (isEditing && detailState.device != null && !loaded) {
            val d = detailState.device!!
            name = d.name ?: ""
            area = d.area ?: ""
            val parts = (d.location ?: "").split(",").map { it.trim() }
            lng = parts.getOrElse(0) { "" }
            lat = parts.getOrElse(1) { "" }
            loaded = true
        }
    }

    // Load map locations when entering map picker
    LaunchedEffect(showMapPicker) {
        if (showMapPicker) viewModel.loadMapLocations()
    }

    // Initialize pending marker from existing coordinates or default
    LaunchedEffect(showMapPicker) {
        if (showMapPicker && pendingLatLng == null) {
            pendingLatLng = if (lng.isNotBlank() && lat.isNotBlank()) {
                LatLng(lat.toDoubleOrNull() ?: 29.56, lng.toDoubleOrNull() ?: 106.56)
            } else {
                LatLng(29.56, 106.56) // default: Chongqing center
            }
        }
    }

    // ── Map Picker Screen ──
    if (showMapPicker) {
        Scaffold(
            containerColor = DarkBlue,
            topBar = {
                TopAppBar(
                    title = { Text("地图选点", fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { showMapPicker = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary)
                )
            },
            bottomBar = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("选中位置", fontSize = 12.sp, color = TextMuted)
                                val latText = pendingLatLng?.latitude?.let { "%.6f".format(it) } ?: "—"
                                val lngText = pendingLatLng?.longitude?.let { "%.6f".format(it) } ?: "—"
                                Text(
                                    "$lngText, $latText",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Cyan
                                )
                            }
                            Button(
                                onClick = {
                                    pendingLatLng?.let {
                                        lng = "%.6f".format(it.longitude)
                                        lat = "%.6f".format(it.latitude)
                                    }
                                    showMapPicker = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("确认位置", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(DarkBlue)
            ) {
                // Area filter chips
                val areas = mapState.mapDevices.map { it.area ?: "" }.filter { it.isNotBlank() }.distinct()
                if (areas.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = mapSelectedArea.isBlank(),
                            onClick = { mapSelectedArea = "" },
                            label = { Text("全部", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        areas.forEach { a ->
                            FilterChip(
                                selected = mapSelectedArea == a,
                                onClick = { mapSelectedArea = if (mapSelectedArea == a) "" else a },
                                label = { Text(a, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                if (mapState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Cyan)
                    }
                } else {
                    AmapView(
                        devices = mapState.mapDevices,
                        selectedArea = mapSelectedArea,
                        modifier = Modifier.fillMaxSize(),
                        onDeviceClick = { },
                        onMapClick = { clickedLng, clickedLat ->
                            pendingLatLng = LatLng(clickedLat, clickedLng)
                        },
                        pendingMarker = pendingLatLng,
                        pendingMarkerTitle = "新建设备位置"
                    )
                }
            }
        }

        return
    }

    // ── Form Screen ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(Brush.verticalGradient(listOf(DarkBlue, Navy)))
    ) {
        TopAppBar(
            title = { Text(if (isEditing) "编辑设备" else "新增设备", fontSize = 18.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary)
        )

        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = devId, onValueChange = { devId = it; error = null },
                label = { Text("设备编号 *") }, enabled = !isEditing,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = fieldColors(), shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("设备名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = fieldColors(), shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = area, onValueChange = { area = it },
                label = { Text("所属区域") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = fieldColors(), shape = RoundedCornerShape(8.dp)
            )

            // Factory serial number
            val fsRemaining = 30 - factorySerial.length
            val fsCounterColor = when {
                fsRemaining < 0 -> Red
                fsRemaining <= 5 -> Amber
                else -> TextMuted
            }
            OutlinedTextField(
                value = factorySerial,
                onValueChange = { if (it.length <= 30) factorySerial = it },
                label = { Text("出厂编号 *") },
                placeholder = { Text("请输入设备出厂编号", fontSize = 12.sp, color = TextMuted.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = fieldColors(), shape = RoundedCornerShape(8.dp),
                supportingText = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("用于生成MQTT鉴权密码", fontSize = 11.sp, color = TextMuted)
                        Text(
                            "${factorySerial.length}/30",
                            fontSize = 11.sp,
                            color = fsCounterColor
                        )
                    }
                }
            )

            // Longitude / Latitude with map picker
            Text("设备位置", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = lng,
                    onValueChange = { lng = it },
                    label = { Text("经度") },
                    placeholder = { Text("例: 106.5622", fontSize = 12.sp, color = TextMuted.copy(alpha = 0.5f)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = lat,
                    onValueChange = { lat = it },
                    label = { Text("纬度") },
                    placeholder = { Text("例: 29.5621", fontSize = 12.sp, color = TextMuted.copy(alpha = 0.5f)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Map picker button
            OutlinedButton(
                onClick = {
                    showMapPicker = true
                    // Preserve current coordinates if any
                    pendingLatLng = if (lng.isNotBlank() && lat.isNotBlank()) {
                        LatLng(lat.toDoubleOrNull() ?: 29.56, lng.toDoubleOrNull() ?: 106.56)
                    } else null
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                border = BorderStroke(1.dp, Cyan)
            ) {
                Icon(Icons.Default.LocationOn, null, tint = Cyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("地图选点", fontSize = 14.sp)
            }

            if (error != null) Text(error!!, color = Red, fontSize = 13.sp)

            Button(
                onClick = {
                    if (devId.isBlank()) { error = "请输入设备编号"; return@Button }
                    if (!isEditing && factorySerial.isBlank()) { error = "请输入出厂编号"; return@Button }
                    submitting = true
                    if (isEditing) {
                        viewModel.updateDevice(deviceId, name, area, lng, lat) { ok ->
                            submitting = false
                            if (ok) onBack() else error = "保存失败"
                        }
                    } else {
                        viewModel.createDevice(devId, name, area, lng, lat, factorySerial) { ok, errMsg ->
                            submitting = false
                            if (ok) onBack() else error = errMsg ?: "创建失败"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !submitting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (isEditing) "保存修改" else "确定新增", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = Color(0x33E2E8F0),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = Cyan,
    unfocusedLabelColor = TextMuted
)
