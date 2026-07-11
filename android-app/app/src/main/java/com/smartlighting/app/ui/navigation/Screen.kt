package com.smartlighting.app.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object Devices : Screen("devices")
    data object DeviceDetail : Screen("devices/{deviceId}") {
        fun createRoute(deviceId: String) = "devices/$deviceId"
    }
    data object DeviceForm : Screen("devices/form/{deviceId}") {
        fun createRoute(deviceId: String = "") = "devices/form/$deviceId"
    }
    data object Alarms : Screen("alarms")
    data object AlarmDetail : Screen("alarms/{alarmId}") {
        fun createRoute(alarmId: Long) = "alarms/$alarmId"
    }
    data object Events : Screen("events")
    data object Analytics : Screen("analytics")
    data object Assistant : Screen("assistant")
}
