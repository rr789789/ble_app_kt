package com.example.thermometer.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val SCAN = "scan"
    const val DEVICE_DETAIL = "device/{deviceMac}"
    const val HISTORY = "history/{deviceMac}"

    fun deviceDetail(mac: String) = "device/$mac"
    fun history(mac: String) = "history/$mac"
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SCAN) {
        composable(Routes.SCAN) {
            ScanScreen(
                onDeviceSelected = { mac ->
                    navController.navigate(Routes.deviceDetail(mac))
                }
            )
        }

        composable(
            route = Routes.DEVICE_DETAIL,
            arguments = listOf(navArgument("deviceMac") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceMac = backStackEntry.arguments?.getString("deviceMac") ?: ""
            DeviceDetailScreen(
                deviceMac = deviceMac,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { mac ->
                    navController.navigate(Routes.history(mac))
                }
            )
        }

        composable(
            route = Routes.HISTORY,
            arguments = listOf(navArgument("deviceMac") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceMac = backStackEntry.arguments?.getString("deviceMac") ?: ""
            HistoryScreen(
                deviceMac = deviceMac,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
