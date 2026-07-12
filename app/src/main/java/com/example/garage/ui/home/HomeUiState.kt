package com.example.garage.ui.home

import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.domain.model.TaskUrgency
import com.example.garage.domain.model.Vehicle

data class TaskWithUrgency(
    val task: MaintenanceTask,
    val urgency: TaskUrgency,
    // Populated only when the user has more than one vehicle, so task rows
    // can show which car a due item belongs to.
    val vehicleLabel: String? = null
)

data class VehicleOverview(
    val vehicle: Vehicle,
    val dueTaskCount: Int,
    val nextDueTask: TaskWithUrgency?
)

data class HomeUiState(
    val greeting: String = "Welcome",
    val userName: String? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleOverviews: List<VehicleOverview> = emptyList(),
    val dueTasks: List<TaskWithUrgency> = emptyList(),
    val recentRecords: List<ServiceRecord> = emptyList(),
    val upcomingReminders: List<TaskWithUrgency> = emptyList(),
    val totalMileage: Int = 0,
    val totalSpent: Double = 0.0,
    val costThisMonth: Double = 0.0,
    val totalRecords: Int = 0,
    val isLoading: Boolean = true
) {
    val hasVehicles: Boolean get() = vehicles.isNotEmpty()
}

val Vehicle.displayName: String
    get() = buildString {
        append(year)
        append(' ')
        append(make)
        append(' ')
        append(model)
        if (!submodel.isNullOrBlank()) {
            append(' ')
            append(submodel)
        }
    }