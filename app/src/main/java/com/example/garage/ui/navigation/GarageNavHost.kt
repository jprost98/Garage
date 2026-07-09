package com.example.garage.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.garage.ui.auth.LoginScreen
import com.example.garage.ui.auth.RegisterScreen
import com.example.garage.ui.components.ComingSoonScreen
import com.example.garage.ui.components.GarageScaffold
import com.example.garage.ui.home.HomeScreen
import com.example.garage.ui.profile.ProfileScreen
import com.example.garage.ui.vehicles.AddVehicleScreen
import com.example.garage.ui.vehicles.VehiclesScreen

/**
 * Phase 2: Vehicles + Add Vehicle are now real. Checkups is still a stub
 * so the bottom nav doesn't crash on tap. Log Record, Maintenance, and
 * Add Task arrive in later phases - see README for the plan. Tapping a
 * vehicle in the list doesn't go anywhere yet (no detail screen built) -
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
                onFabAction = { navController.navigate(Routes.ADD_VEHICLE) }
            ) { contentModifier ->
                HomeScreen(
                    modifier = contentModifier,
                    onAddVehicle = { navController.navigate(Routes.ADD_VEHICLE) },
                    onTaskClick = { navigateTopLevel(Routes.MAINTENANCE) }
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
                    onVehicleClick = { /* vehicle detail screen isn't built yet */ }
                )
            }
        }

        composable(Routes.ADD_VEHICLE) {
            AddVehicleScreen(
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
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
                    onSignedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
                )
            }
        }
    }
}
