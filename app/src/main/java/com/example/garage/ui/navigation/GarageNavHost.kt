package com.example.garage.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.garage.ui.archive.ArchiveScreen
import com.example.garage.ui.auth.LoginScreen
import com.example.garage.ui.auth.RegisterScreen
import com.example.garage.ui.components.ComingSoonScreen
import com.example.garage.ui.components.GarageScaffold
import com.example.garage.ui.home.HomeScreen
import com.example.garage.ui.profile.ProfileScreen
import com.example.garage.ui.record.LogRecordScreen
import com.example.garage.ui.record.RecordDetailScreen
import com.example.garage.ui.vehicles.AddVehicleScreen
import com.example.garage.ui.vehicles.VehicleDetailScreen
import com.example.garage.ui.vehicles.VehiclesScreen

/**
 * Phase 3: Vehicles, Add Vehicle, Vehicle Detail, Log Record, and Record
 * Detail are all wired up now. Maintenance/Add Task are still stubs -
 * that's next.
 */
@Composable
fun GarageNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onGoToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            GarageScaffold(
                currentRoute = Routes.HOME,
                onNavigate = ::navigateTopLevel,
                onFabAction = { },
                showFab = false
            ) { contentModifier ->
                HomeScreen(
                    modifier = contentModifier,
                    onTaskClick = { navigateTopLevel(Routes.MAINTENANCE) },
                    onRecordClick = { recordId -> navController.navigate(Routes.recordDetail(recordId)) },
                    onAddVehicle = { navController.navigate(Routes.ADD_VEHICLE) }
                )
            }
        }

        composable(Routes.VEHICLES) {
            GarageScaffold(
                currentRoute = Routes.VEHICLES,
                onNavigate = ::navigateTopLevel,
                onFabAction = { navController.navigate(Routes.ADD_VEHICLE) }
            ) { contentModifier ->
                VehiclesScreen(
                    modifier = contentModifier,
                    onAddVehicle = { navController.navigate(Routes.ADD_VEHICLE) },
                    onVehicleClick = { vehicleId -> navController.navigate(Routes.vehicleDetail(vehicleId)) },
                    onRecordClick = { recordId -> navController.navigate(Routes.recordDetail(recordId)) }
                )
            }
        }

        composable(
            route = Routes.ADD_VEHICLE,
            arguments = listOf(navArgument("vehicleId") { type = NavType.StringType; nullable = true })
        ) {
            AddVehicleScreen(
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.VEHICLE_DETAIL,
            arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
        ) {
            VehicleDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { vehicleId -> navController.navigate(Routes.addVehicle(vehicleId)) },
                onLogRecord = { vehicleId -> navController.navigate(Routes.logRecord(vehicleId)) },
                onRecordClick = { recordId -> navController.navigate(Routes.recordDetail(recordId)) }
            )
        }

        composable(
            route = Routes.LOG_RECORD,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.StringType },
                navArgument("recordId") { type = NavType.StringType; nullable = true }
            )
        ) {
            LogRecordScreen(
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.RECORD_DETAIL,
            arguments = listOf(navArgument("recordId") { type = NavType.StringType })
        ) {
            RecordDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { vehicleId, recordId -> navController.navigate(Routes.logRecord(vehicleId, recordId)) }
            )
        }

        composable(Routes.MAINTENANCE) {
            GarageScaffold(
                currentRoute = Routes.MAINTENANCE,
                onNavigate = ::navigateTopLevel,
                onFabAction = { }
            ) { contentModifier ->
                ComingSoonScreen(title = "Maintenance", modifier = contentModifier)
            }
        }

        composable(Routes.PROFILE) {
            GarageScaffold(
                currentRoute = Routes.PROFILE,
                onNavigate = ::navigateTopLevel,
                onFabAction = { }
            ) { contentModifier ->
                ProfileScreen(
                    modifier = contentModifier,
                    onSignedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } },
                    onNavigateToArchive = { navController.navigate(Routes.ARCHIVE) }
                )
            }
        }

        composable(Routes.ARCHIVE) {
            ArchiveScreen(
                onBack = { navController.popBackStack() },
                onVehicleClick = { vehicleId -> navController.navigate(Routes.vehicleDetail(vehicleId)) },
                onRecordClick = { recordId -> navController.navigate(Routes.recordDetail(recordId)) }
            )
        }
    }
}
