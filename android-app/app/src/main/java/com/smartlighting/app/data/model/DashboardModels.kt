package com.smartlighting.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DashboardStats(
    @Json(name = "totalDevices") val totalDevices: Long = 0,
    @Json(name = "onlineDevices") val onlineDevices: Long = 0,
    @Json(name = "onlineRate") val onlineRate: String = "0",
    @Json(name = "alertCount") val alertCount: Long = 0,
    @Json(name = "energySavingRate") val energySavingRate: Double = 0.0,
    @Json(name = "todayEnergy") val todayEnergy: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class EnergyTrend(
    @Json(name = "labels") val labels: List<String> = emptyList(),
    @Json(name = "current") val current: List<Double> = emptyList(),
    @Json(name = "lastWeek") val lastWeek: List<Double> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DistrictStatus(
    @Json(name = "name") val name: String = "",
    @Json(name = "online") val online: Long = 0,
    @Json(name = "offline") val offline: Long = 0,
    @Json(name = "warning") val warning: Long = 0,
    @Json(name = "disabled") val disabled: Long = 0
)

@JsonClass(generateAdapter = true)
data class HealthSummary(
    @Json(name = "totalDevices") val totalDevices: Long = 0,
    @Json(name = "healthyCount") val healthyCount: Long = 0,
    @Json(name = "warningCount") val warningCount: Long = 0,
    @Json(name = "criticalCount") val criticalCount: Long = 0,
    @Json(name = "averageScore") val averageScore: Double = 0.0
)
