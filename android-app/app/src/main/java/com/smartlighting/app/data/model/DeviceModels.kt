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
        get() = when (parseLatestField(latestData, "action")?.toString()) {
            "ON" -> true; "OFF" -> false; else -> null
        }

    /** 当前亮度 0-100，null 表示未知 */
    val brightness: Int?
        get() = (parseLatestField(latestData, "brightness") as? Number)?.toInt()

    /** 控制来源：MANUAL/AUTO */
    val controlSource: String?
        get() = parseLatestField(latestData, "controlSource")?.toString()
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
    @Json(name = "enabled") val enabled: Boolean = true
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
