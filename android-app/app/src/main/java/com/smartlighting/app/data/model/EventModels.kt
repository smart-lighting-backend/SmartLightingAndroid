package com.smartlighting.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VisionEvent(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "eventType") val eventType: String = "",
    @Json(name = "confidence") val confidence: Double = 0.0,
    @Json(name = "snapshotRef") val snapshotRef: String? = null,
    @Json(name = "occurredAt") val occurredAt: String? = null
)

@JsonClass(generateAdapter = true)
data class VoiceEvent(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "type") val type: String = "",
    @Json(name = "content") val content: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "occurredAt") val occurredAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "type") val type: String = "",
    @Json(name = "content") val content: String = "",
    @Json(name = "action") val action: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class DiagnoseRequest(
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "question") val question: String? = null
)
