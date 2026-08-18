package com.smartlighting.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class Device(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "name") val name: String? = null,
    @Json(name = "area") val area: String? = null,
    @Json(name = "areaId") val areaId: Long? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "status") val status: Int = 0,
    @Json(name = "healthScore") val healthScore: Double = 0.0,
    @Json(name = "topicPrefix") val topicPrefix: String? = null,
    @Json(name = "lastHeartbeatAt") val lastHeartbeatAt: String? = null,
    @Json(name = "enabled") val enabled: Boolean = true,
    @Json(name = "ratedPower") val ratedPower: Double? = null,
    @Json(name = "latestData") val latestData: String? = null,
    @Json(name = "manualMode") val manualMode: Boolean? = null,
    @Json(name = "manualExpireAt") val manualExpireAt: String? = null
) {
    companion object {
        private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        private val mapAdapter = moshi.adapter(Map::class.java)

        fun parseLatestField(latestData: String?, key: String): Any? {
            if (latestData == null) return null
            return try {
                val map = mapAdapter.fromJson(latestData) as? Map<*, *>
                map?.get(key)
            } catch (_: Exception) { null }
        }
    }

    /** 开关灯状态：true=开灯, false=关灯, null=未知 */
    val lightOn: Boolean?
        get() {
            /* 模拟设备：action 字段; 真实硬件 BearPi：led_status 字段 */
            val action = parseLatestField(latestData, "action")?.toString()
            if (action != null) return when (action) { "ON" -> true; "OFF" -> false; else -> null }
            val led = parseLatestField(latestData, "led_status")?.toString()
            if (led != null) return led == "ON"
            return null
        }

    /** 当前亮度 0-100，null 表示未知 */
    val brightness: Int?
        get() = (parseLatestField(latestData, "brightness") as? Number)?.toInt()

    /** 控制来源：MANUAL/AUTO（兼容 controlSource 和 led_source） */
    val controlSource: String?
        get() {
            val cs = parseLatestField(latestData, "controlSource")?.toString()
            if (cs != null) return cs
            val ls = parseLatestField(latestData, "led_source")?.toString()
            if (ls != null) return ls.uppercase()
            return null
        }
}

@JsonClass(generateAdapter = true)
data class CreateDeviceRequest(
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "area") val area: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "status") val status: Int = 1,
    @Json(name = "healthScore") val healthScore: Int = 100,
    @Json(name = "topicPrefix") val topicPrefix: String = "streetlight",
    @Json(name = "enabled") val enabled: Boolean = true,
    @Json(name = "factorySerial") val factorySerial: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviceMapLocation(
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "location") val location: String? = null,
    @Json(name = "status") val status: Int = 0,
    @Json(name = "area") val area: String? = null,
    @Json(name = "healthScore") val healthScore: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class MapLocationsResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "msg") val msg: String,
    @Json(name = "data") val data: List<DeviceMapLocation> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LatestTelemetry(
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "area") val area: String = "",
    @Json(name = "status") val status: Int = 0,
    @Json(name = "healthScore") val healthScore: Double = 0.0,
    @Json(name = "data") val data: TelemetryData? = null,
    @Json(name = "lastHeartbeatAt") val lastHeartbeatAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TelemetryData(
    @Json(name = "illuminance") val illuminance: Double? = null,
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "humidity") val humidity: Double? = null,
    @Json(name = "pm25") val pm25: Double? = null,
    @Json(name = "aqi") val aqi: Double? = null,
    @Json(name = "pir") val pir: Int? = null,
    @Json(name = "trafficFlow") val trafficFlow: Double? = null
)

@JsonClass(generateAdapter = true)
data class DeviceHealth(
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "deviceName") val deviceName: String = "",
    @Json(name = "overallScore") val overallScore: Double = 0.0,
    @Json(name = "level") val level: String = "",
    @Json(name = "levelColor") val levelColor: String = "",
    @Json(name = "suggestion") val suggestion: String = ""
)

@JsonClass(generateAdapter = true)
data class ControlRequest(
    @Json(name = "action") val action: String,
    @Json(name = "brightness") val brightness: Int? = null
)

@JsonClass(generateAdapter = true)
data class ControlHistoryItem(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "action") val action: String = "",
    @Json(name = "brightness") val brightness: Int? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "operator") val operator: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "issuedAt") val issuedAt: String? = null
)

// ── Batch Operation Models ────────────────────────────────────

@JsonClass(generateAdapter = true)
data class BatchDeviceRequest(
    @Json(name = "deviceIds") val deviceIds: List<Long>
)

@JsonClass(generateAdapter = true)
data class BatchCreateResult(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "success") val success: Int = 0,
    @Json(name = "failed") val failed: Int = 0,
    @Json(name = "failedDetails") val failedDetails: List<BatchErrorDetail> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BatchErrorDetail(
    @Json(name = "row") val row: Int = 0,
    @Json(name = "deviceId") val deviceId: String? = null,
    @Json(name = "reason") val reason: String = ""
)

@JsonClass(generateAdapter = true)
data class BatchOperationResult(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "success") val success: Int = 0,
    @Json(name = "failed") val failed: Int = 0,
    @Json(name = "failedDetails") val failedDetails: List<BatchOpErrorDetail> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BatchOpErrorDetail(
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "reason") val reason: String = ""
)

/** 客户端解析用：表示一行待导入的设备数据及校验状态 */
data class ImportRow(
    val rowNum: Int,
    val deviceId: String,
    val name: String = "",
    val area: String = "",
    val longitude: String = "",
    val latitude: String = "",
    val factorySerial: String = "",
    val errors: List<String> = emptyList()
) {
    val valid: Boolean get() = errors.isEmpty()
    val location: String? get() {
        val lng = longitude.toDoubleOrNull()
        val lat = latitude.toDoubleOrNull()
        return if (lng != null && lat != null) "$lng,$lat" else null
    }
}
