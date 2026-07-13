package com.example.garage.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.example.garage.ui.components.GarageAssistantSheet
import com.example.garage.ui.components.GarageScaffold
import com.example.garage.ui.home.HomeScreen
import com.example.garage.ui.maintenance.AddMaintenanceTaskScreen
import com.example.garage.ui.maintenance.MaintenanceScreen
import com.example.garage.ui.maintenance.TaskDetailScreen
import com.example.garage.ui.profile.ProfileScreen
import com.example.garage.ui.record.LogRecordScreen
import com.example.garage.ui.record.RecordDetailScreen
import com.example.garage.ui.vehicles.AddVehicleScreen
import com.example.garage.ui.vehicles.VehicleDetailScreen
import com.example.garage.ui.vehicles.VehiclesScreen

/**
 * Phase 3: Vehicles, Add Vehicle, Vehicle Detail, Log Record, Record
 * Detail, and Checkups (Maintenance) are all wired up now. Add Task is
 * still a stub - that's next.
 */
@Composable
fun GarageNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var showAssistant by remember { mutableStateOf(false) }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (showAssistant) {
        GarageAssistantSheet(onDismiss = { showAssistant = false })
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
                showFab = false,
                onAssistantClick = { showAssistant = true }
            ) { contentModifier ->
                HomeScreen(
                    modifier = contentModifier,
                    onTaskClick = { taskId -> navController.navigate(Routes.taskDetail(taskId)) },
                    onRecordClick = { recordId -> navController.navigate(Routes.recordDetail(recordId)) },
                    onAddVehicle = { navController.navigate(Routes.ADD_VEHICLE) },
                    onLogRecord = { vehicleId, scan -> navController.navigate(Routes.logRecord(vehicleId, scan = scan)) }
                )
            }
        }

        composable(Routes.VEHICLES) {
            GarageScaffold(
                currentRoute = Routes.VEHICLES,
                onNavigate = ::navigateTopLevel,
                onFabAction = { navController.navigate(Routes.ADD_VEHICLE) },
                showFab = true,
                onAssistantClick = { showAssistant = true }
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
                navArgument("recordId") { type = NavType.StringType; nullable = true },
                navArgument("scan") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val scan = backStackEntry.arguments?.getBoolean("scan") ?: false
            LogRecordScreen(
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                startScan = scan
            )
        }

        composable(
            route = Routes.ADD_TASK,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.StringType },
                navArgument("taskId") { type = NavType.StringType; nullable = true }
            )
        ) {
            AddMaintenanceTaskScreen(
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

        composable(
            route = Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) {
            TaskDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { vehicleId, taskId -> navController.navigate(Routes.addTask(vehicleId, taskId)) }
            )
        }

        composable(Routes.MAINTENANCE) {
            val viewModel: com.example.garage.ui.maintenance.MaintenanceViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            GarageScaffold(
                currentRoute = Routes.MAINTENANCE,
                onNavigate = ::navigateTopLevel,
                onFabAction = {
                    state.selectedVehicleId?.let { vId ->
                        navController.navigate(Routes.addTask(vId))
                    } ?: state.vehicles.firstOrNull()?.id?.let { vId ->
                        navController.navigate(Routes.addTask(vId))
                    }
                },
                showFab = state.hasVehicles,
                onAssistantClick = { showAssistant = true }
            ) { contentModifier ->
                MaintenanceScreen(
                    modifier = contentModifier,
                    onTaskClick = { vId, tId -> navController.navigate(Routes.taskDetail(tId)) },
                    onAddVehicle = { navController.navigate(Routes.ADD_VEHICLE) },
                    viewModel = viewModel
                )
            }
        }

        composable(Routes.PROFILE) {
            GarageScaffold(
                currentRoute = Routes.PROFILE,
                onNavigate = ::navigateTopLevel,
                onFabAction = { },
                showFab = false,
                onAssistantClick = { showAssistant = true }
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
                onRecordClick = { recordId -> navController.navigate(Routes.recordDetail(recordId)) },
                onTaskClick = { taskId -> navController.navigate(Routes.taskDetail(taskId)) }
            )
        }
    }
}