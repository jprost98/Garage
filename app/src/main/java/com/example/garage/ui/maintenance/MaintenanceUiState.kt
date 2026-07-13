package com.example.garage.ui.maintenance

import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.domain.model.TaskUrgency
import com.example.garage.domain.model.Vehicle

/**
 * A task paired with its freshly-derived urgency and, when there's more
 * than one vehicle in play, a label so the row can show which car it
 * belongs to.
 */
data class TaskWithUrgency(
    val task: MaintenanceTask,
    val urgency: TaskUrgency,
    val vehicleLabel: String? = null
)

data class MaintenanceUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: String? = null,
    val overdueTasks: List<TaskWithUrgency> = emptyList(),
    val dueSoonTasks: List<TaskWithUrgency> = emptyList(),
    val upcomingTasks: List<TaskWithUrgency> = emptyList(),
    val upToDateTasks: List<TaskWithUrgency> = emptyList(),
    val completedTasks: List<TaskWithUrgency> = emptyList(),
    val showCompleted: Boolean = false,
    val hasAnyTasks: Boolean = false,
    val isLoading: Boolean = true,
    val pendingRecordReview: ServiceRecord? = null,
    val pendingTaskToComplete: MaintenanceTask? = null,
    val showUndoCompletionWarning: MaintenanceTask? = null
) {
    val hasVehicles: Boolean get() = vehicles.isNotEmpty()
}
