package com.smartlighting.app.ui.components

import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.smartlighting.app.data.model.DeviceMapLocation
import kotlinx.coroutines.delay

@Composable
fun AmapView(
    devices: List<DeviceMapLocation>,
    selectedArea: String = "",
    modifier: Modifier = Modifier,
    onDeviceClick: (String) -> Unit = {},
    onMapClick: ((Double, Double) -> Unit)? = null,
    pendingMarker: LatLng? = null,
    pendingMarkerTitle: String = ""
) {
    var aMap by remember { mutableStateOf<AMap?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var allMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var areaMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var pulseOn by remember { mutableStateOf(false) }
    var pendingMapMarker by remember { mutableStateOf<Marker?>(null) }

    // Area color palette
    val areaColors = remember(devices) {
        val palette = listOf(
            AndroidColor.rgb(37, 99, 235), AndroidColor.rgb(22, 160, 133),
            AndroidColor.rgb(230, 147, 40), AndroidColor.rgb(122, 92, 190),
            AndroidColor.rgb(221, 72, 72), AndroidColor.rgb(8, 145, 178),
            AndroidColor.rgb(147, 51, 234), AndroidColor.rgb(219, 39, 119),
            AndroidColor.rgb(234, 88, 12), AndroidColor.rgb(79, 70, 229)
        )
        val map = mutableMapOf<String, Int>()
        var idx = 0
        devices.map { it.area ?: "" }.distinct().filter { it.isNotBlank() }.forEach { area ->
            map[area] = palette[idx % palette.size]; idx++
        }
        map.toMap()
    }

    fun getDeviceColor(d: DeviceMapLocation): Int {
        if (d.status == 0 || d.status == 2) return AndroidColor.rgb(107, 127, 147)
        if (d.status == 3) return AndroidColor.rgb(245, 158, 11)
        return areaColors[d.area ?: ""] ?: AndroidColor.rgb(38, 166, 218)
    }

    // Pulse animation for selected area
    LaunchedEffect(selectedArea, mapReady) {
        if (!mapReady || selectedArea.isBlank() || areaMarkers.isEmpty()) {
            pulseOn = false
            return@LaunchedEffect
        }
        while (true) {
            pulseOn = !pulseOn
            areaMarkers.forEach { m ->
                val devData = devices.find { d -> d.deviceId == m.snippet?.split("|")?.firstOrNull() }
                if (devData != null) {
                    val color = getDeviceColor(devData)
                    m.setIcon(StreetlightIcon.create(color, StreetlightIcon.statusLabel(devData.status), pulseOn))
                }
            }
            delay(600)
        }
    }

    // When area changes, rebuild dim/highlight states
    LaunchedEffect(selectedArea, allMarkers, mapReady) {
        if (!mapReady) return@LaunchedEffect
        allMarkers.forEach { m ->
            val devData = devices.find { d -> d.deviceId == m.snippet?.split("|")?.firstOrNull() }
            if (devData == null) return@forEach
            val isInArea = selectedArea.isBlank() || devData.area == selectedArea
            m.setZIndex(if (isInArea) 200f else 40f)

            // Redraw icon with dim/bright state
            val color = if (isInArea || selectedArea.isBlank()) getDeviceColor(devData)
            else AndroidColor.rgb(80, 90, 100) // dim
            m.setIcon(StreetlightIcon.create(color, StreetlightIcon.statusLabel(devData.status), false))
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                onCreate(null)
                Handler(Looper.getMainLooper()).postDelayed({
                    this.map?.let { map ->
                        aMap = map
                        mapReady = true
                        map.uiSettings.isZoomControlsEnabled = false
                        if (onMapClick != null) {
                            map.setOnMapClickListener { latLng ->
                                onMapClick(latLng.longitude, latLng.latitude)
                            }
                        }
                        map.setOnMarkerClickListener { marker ->
                            marker.showInfoWindow()
                            true
                        }
                        map.setOnInfoWindowClickListener { marker ->
                            val deviceId = marker.snippet?.split("|")?.firstOrNull() ?: return@setOnInfoWindowClickListener
                            onDeviceClick(deviceId)
                        }
                        map.setInfoWindowAdapter(object : AMap.InfoWindowAdapter {
                            override fun getInfoWindow(marker: Marker): android.view.View? = null
                            override fun getInfoContents(marker: Marker): android.view.View {
                                val parts = marker.snippet?.split("|") ?: listOf("", "", "", "")
                                val devId = parts.getOrElse(0) { "" }
                                val loc = parts.getOrElse(1) { "" }
                                val area = parts.getOrElse(2) { "" }
                                val status = parts.getOrElse(3) { "1" }.toIntOrNull() ?: 1
                                val st = StreetlightIcon.statusLabel(status)
                                val stColor = when (status) {
                                    1 -> AndroidColor.rgb(22, 160, 133); 2 -> AndroidColor.rgb(148, 163, 184)
                                    3 -> AndroidColor.rgb(245, 158, 11); else -> AndroidColor.rgb(148, 163, 184)
                                }
                                return android.widget.LinearLayout(ctx).apply {
                                    orientation = android.widget.LinearLayout.VERTICAL
                                    setPadding(18, 14, 18, 14)
                                    addView(android.widget.TextView(ctx).apply {
                                        text = marker.title; textSize = 15f; setTextColor(AndroidColor.rgb(15, 23, 42))
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    })
                                    addView(android.widget.TextView(ctx).apply {
                                        text = "$devId · $area"; textSize = 12f; setTextColor(AndroidColor.rgb(107, 114, 128))
                                    })
                                    addView(android.widget.TextView(ctx).apply {
                                        text = "坐标: $loc"; textSize = 11f; setTextColor(AndroidColor.rgb(148, 163, 184))
                                    })
                                    addView(android.widget.TextView(ctx).apply {
                                        text = "状态: $st"; textSize = 12f; setTextColor(stColor)
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    })
                                    addView(android.widget.TextView(ctx).apply {
                                        text = "点击查看详情 →"; textSize = 13f; setTextColor(AndroidColor.rgb(37, 99, 235))
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        setPadding(0, 10, 0, 0)
                                    })
                                }
                            }
                        })
                    }
                }, 500)
            }
        },
        modifier = modifier
    )

    // Render markers on map
    LaunchedEffect(mapReady, devices, selectedArea) {
        if (!mapReady) return@LaunchedEffect
        aMap?.let { map ->
            map.clear()
            allMarkers = emptyList()
            areaMarkers = emptyList()

            if (devices.isEmpty()) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(29.5621, 106.5622), 13f))
                return@LaunchedEffect
            }

            val valid = devices.filter { it.location != null && it.location.contains(",") }
            val newAreaMarkers = mutableListOf<Marker>()
            val newAllMarkers = mutableListOf<Marker>()

            valid.forEach { d ->
                val loc = d.location!!
                val parts = loc.split(",")
                val lng = parts[0].trim().toDoubleOrNull() ?: return@forEach
                val lat = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return@forEach

                val isInArea = selectedArea.isBlank() || d.area == selectedArea
                val color = if (isInArea || selectedArea.isBlank()) getDeviceColor(d)
                else AndroidColor.rgb(80, 90, 100)
                val status = StreetlightIcon.statusLabel(d.status)
                val icon = StreetlightIcon.create(color, status, false)

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(lat, lng))
                        .title(d.name.ifBlank { d.deviceId })
                        .snippet("${d.deviceId}|${d.location ?: "-"}|${d.area ?: "-"}|${d.status}")
                        .icon(icon)
                        .zIndex(if (isInArea) 200f else 100f)
                        .anchor(0.5f, 0.85f)
                )
                marker?.let { m ->
                    newAllMarkers.add(m)
                    if (selectedArea.isNotBlank() && d.area == selectedArea)
                        newAreaMarkers.add(m)
                }
            }

            allMarkers = newAllMarkers
            areaMarkers = newAreaMarkers

            // Zoom: fit selected area or all markers
            if (selectedArea.isNotBlank() && newAreaMarkers.isNotEmpty()) {
                val builder = LatLngBounds.Builder()
                newAreaMarkers.forEach { builder.include(it.position) }
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
            } else if (newAllMarkers.isNotEmpty()) {
                val builder = LatLngBounds.Builder()
                newAllMarkers.take(60).forEach { builder.include(it.position) }
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 80))
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(29.5621, 106.5622), 13f))
            }
        }
    }

    // Pending marker — independent of device markers
    LaunchedEffect(mapReady, pendingMarker, pendingMarkerTitle) {
        if (!mapReady) return@LaunchedEffect
        aMap?.let { map ->
            pendingMapMarker?.remove()
            pendingMapMarker = null
            pendingMarker?.let { pm ->
                val redIcon = StreetlightIcon.create(AndroidColor.rgb(221, 72, 72), "NEW", true)
                pendingMapMarker = map.addMarker(
                    MarkerOptions()
                        .position(pm)
                        .title(pendingMarkerTitle.ifBlank { "新建设备" })
                        .snippet("点击地图移动位置")
                        .icon(redIcon)
                        .zIndex(300f)
                        .anchor(0.5f, 0.85f)
                )
            }
        }
    }
}
