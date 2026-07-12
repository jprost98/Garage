package com.example.garage.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val VEHICLES = "vehicles"
    const val ADD_VEHICLE = "add_vehicle?vehicleId={vehicleId}"
    const val VEHICLE_DETAIL = "vehicle_detail/{vehicleId}"
    const val MAINTENANCE = "maintenance"
    const val ADD_TASK = "add_task/{vehicleId}"
    const val LOG_RECORD = "log_record/{vehicleId}?recordId={recordId}"
    const val RECORD_DETAIL = "record_detail/{recordId}"
    const val PROFILE = "profile"
    const val ARCHIVE = "archive"

    fun addVehicle(vehicleId: String? = null) = if (vehicleId != null) "add_vehicle?vehicleId=$vehicleId" else "add_vehicle"
    fun vehicleDetail(vehicleId: String) = "vehicle_detail/$vehicleId"
    fun addTask(vehicleId: String) = "add_task/$vehicleId"
    fun logRecord(vehicleId: String, recordId: String? = null) = 
        if (recordId != null) "log_record/$vehicleId?recordId=$recordId" else "log_record/$vehicleId"
    fun recordDetail(recordId: String) = "record_detail/$recordId"
}

/** Destinations that show the bottom nav bar + FAB. */
val topLevelRoutes = setOf(Routes.HOME, Routes.VEHICLES, Routes.MAINTENANCE, Routes.PROFILE)
