package com.example.garage.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val VEHICLES = "vehicles"
    const val ADD_VEHICLE = "add_vehicle"
    const val MAINTENANCE = "maintenance"
    const val ADD_TASK = "add_task/{vehicleId}"
    const val LOG_RECORD = "log_record/{vehicleId}"
    const val PROFILE = "profile"

    fun addTask(vehicleId: String) = "add_task/$vehicleId"
    fun logRecord(vehicleId: String) = "log_record/$vehicleId"
}

/** Destinations that show the bottom nav bar + FAB. */
val topLevelRoutes = setOf(Routes.HOME, Routes.VEHICLES, Routes.MAINTENANCE, Routes.PROFILE)
