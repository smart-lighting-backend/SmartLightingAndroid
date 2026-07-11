package com.smartlighting.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlarmRecord(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "type") val type: String = "",
    @Json(name = "level") val level: String = "",
    @Json(name = "status") val status: String = "",
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "startAt") val startAt: String? = null,
    @Json(name = "recoverAt") val recoverAt: String? = null,
    @Json(name = "handler") val handler: String? = null
)

@JsonClass(generateAdapter = true)
data class AlarmStats(
    @Json(name = "totalActive") val totalActive: Int = 0,
    @Json(name = "byLevel") val byLevel: Map<String, Int> = emptyMap(),
    @Json(name = "byType") val byType: Map<String, Int> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class AlarmTrendItem(
    @Json(name = "date") val date: String = "",
    @Json(name = "count") val count: Int = 0
)

@JsonClass(generateAdapter = true)
data class HandleAlarmRequest(
    @Json(name = "remark") val remark: String? = null
)
