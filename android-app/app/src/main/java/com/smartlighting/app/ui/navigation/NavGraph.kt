package com.smartlighting.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartlighting.app.ui.alarm.AlarmDetailScreen
import com.smartlighting.app.ui.alarm.AlarmListScreen

import com.smartlighting.app.ui.assistant.AssistantScreen
import com.smartlighting.app.ui.dashboard.DashboardScreen
import com.smartlighting.app.ui.device.DeviceDetailScreen
import com.smartlighting.app.ui.device.DeviceFormScreen
import com.smartlighting.app.ui.device.DeviceListScreen
import com.smartlighting.app.ui.event.EventScreen
import com.smartlighting.app.ui.login.LoginScreen
import com.smartlighting.app.ui.theme.*

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("dashboard", "首页", Icons.Default.Home),
    BottomNavItem("devices", "设备", Icons.Default.DevicesOther),
    BottomNavItem("alarms", "告警", Icons.Default.Warning),
    BottomNavItem("events", "事件", Icons.Default.Event),
    BottomNavItem("assistant", "助手", Icons.Default.SmartToy)
)

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(navController)
        }

        composable(
            Screen.DeviceDetail.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            DeviceDetailScreen(deviceId = deviceId, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.DeviceForm.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val devId = backStackEntry.arguments?.getString("deviceId") ?: ""
            DeviceFormScreen(deviceId = devId, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.AlarmDetail.route,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
            AlarmDetailScreen(alarmId = alarmId, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = DarkBlue,
        bottomBar = {
            if (currentRoute in bottomNavItems.map { it.route }) {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color(0xFF0F1A2E)) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                tabNavController.navigate(item.route) {
                                    popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Cyan, selectedTextColor = Cyan,
                                unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                                indicatorColor = PrimaryBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = tabNavController,
            startDestination = "dashboard",
            modifier = Modifier
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onDeviceClick = { deviceId ->
                        rootNavController.navigate(Screen.DeviceDetail.createRoute(deviceId))
                    }
                )
            }
            composable("devices") {
                DeviceListScreen(
                    onDeviceClick = { deviceId ->
                        rootNavController.navigate(Screen.DeviceDetail.createRoute(deviceId))
                    },
                    onAddDevice = {
                        rootNavController.navigate(Screen.DeviceForm.createRoute())
                    },
                    onEditDevice = { deviceId ->
                        rootNavController.navigate(Screen.DeviceForm.createRoute(deviceId))
                    }
                )
            }
            composable("alarms") {
                AlarmListScreen(
                    onAlarmClick = { alarmId ->
                        rootNavController.navigate(Screen.AlarmDetail.createRoute(alarmId))
                    }
                )
            }
            composable("events") { EventScreen() }
            composable("assistant") { AssistantScreen() }
        }
    }
}
