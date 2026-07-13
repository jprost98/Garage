package com.example.garage.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val VEHICLES = "vehicles"
    const val ADD_VEHICLE = "add_vehicle?vehicleId={vehicleId}"
    const val VEHICLE_DETAIL = "vehicle_detail/{vehicleId}"
    const val MAINTENANCE = "maintenance"
    const val ADD_TASK = "add_task/{vehicleId}?taskId={taskId}"
    const val LOG_RECORD = "log_record/{vehicleId}?recordId={recordId}&scan={scan}"
    const val RECORD_DETAIL = "record_detail/{recordId}"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val PROFILE = "profile"
    const val ARCHIVE = "archive"

    fun addVehicle(vehicleId: String? = null) = if (vehicleId != null) "add_vehicle?vehicleId=$vehicleId" else "add_vehicle"
    fun vehicleDetail(vehicleId: String) = "vehicle_detail/$vehicleId"
    fun addTask(vehicleId: String, taskId: String? = null) = 
        if (taskId != null) "add_task/$vehicleId?taskId=$taskId" else "add_task/$vehicleId"
    fun logRecord(vehicleId: String, recordId: String? = null, scan: Boolean = false) = buildString {
        append("log_record/$vehicleId")
        val params = mutableListOf<String>()
        if (recordId != null) params.add("recordId=$recordId")
        if (scan) params.add("scan=true")
        if (params.isNotEmpty()) {
            append("?")
            append(params.joinToString("&"))
        }
    }
    fun recordDetail(recordId: String) = "record_detail/$recordId"
    fun taskDetail(taskId: String) = "task_detail/$taskId"
}

/** Destinations that show the bottom nav bar + FAB. */
val topLevelRoutes = setOf(Routes.HOME, Routes.VEHICLES, Routes.MAINTENANCE, Routes.PROFILE)
