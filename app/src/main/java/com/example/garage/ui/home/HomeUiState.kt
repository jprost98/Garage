package com.example.garage.ui.home

import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.domain.model.TaskUrgency
import com.example.garage.domain.model.Vehicle

data class TaskWithUrgency(val task: MaintenanceTask, val urgency: TaskUrgency)

data class HomeUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: String? = null,
    val dueTasks: List<TaskWithUrgency> = emptyList(),
    val recentRecords: List<ServiceRecord> = emptyList(),
    val isLoading: Boolean = true
) {
    val hasVehicles: Boolean get() = vehicles.isNotEmpty()
    val selectedVehicle: Vehicle? get() = vehicles.firstOrNull { it.id == selectedVehicleId }
}
