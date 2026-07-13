package com.example.garage.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.domain.model.TaskUrgency
import com.example.garage.domain.usecase.TaskUrgencyCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Powers the Checkups tab: every active vehicle's maintenance tasks,
 * grouped by urgency and optionally filtered to a single vehicle.
 * Urgency is derived fresh from each vehicle's current odometer rather
 * than stored, same as on Home.
 */
@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val taskRepository: MaintenanceTaskRepository,
    private val serviceRecordRepository: ServiceRecordRepository,
    private val urgencyCalculator: TaskUrgencyCalculator
) : ViewModel() {

    private val selectedVehicleId = MutableStateFlow<String?>(null)
    private val showCompleted = MutableStateFlow(false)
    private val pendingRecordReview = MutableStateFlow<ServiceRecord?>(null)
    private val pendingTaskToComplete = MutableStateFlow<MaintenanceTask?>(null)
    private val showUndoCompletionWarning = MutableStateFlow<MaintenanceTask?>(null)

    val uiState: StateFlow<MaintenanceUiState> = combine(
        vehicleRepository.observeVehicles(),
        taskRepository.observeAll(),
        selectedVehicleId,
        showCompleted,
        pendingRecordReview,
        pendingTaskToComplete,
        showUndoCompletionWarning
    ) { params: Array<Any?> ->
        val allVehicles = params[0] as List<com.example.garage.domain.model.Vehicle>
        val allTasks = params[1] as List<MaintenanceTask>
        val selectedId = params[2] as String?
        val showDone = params[3] as Boolean
        val pendingReview = params[4] as ServiceRecord?
        val pendingTask = params[5] as MaintenanceTask?
        val undoWarning = params[6] as MaintenanceTask?

        val vehicles = allVehicles.filterNot { it.isArchived }
        val activeVehicleIds = vehicles.mapTo(mutableSetOf()) { it.id }
        val odometerByVehicle = vehicles.associate { it.id to it.odometer }
        val vehicleById = vehicles.associateBy { it.id }
        val showVehicleLabels = vehicles.size > 1

        val tasks = allTasks
            .filter { it.vehicleId in activeVehicleIds }
            .filter { selectedId == null || it.vehicleId == selectedId }

        val tasksWithUrgency = tasks.map { task ->
            val odometer = odometerByVehicle[task.vehicleId] ?: 0
            val vehicleLabel = if (showVehicleLabels) vehicleById[task.vehicleId]?.title else null
            TaskWithUrgency(task, urgencyCalculator.urgencyFor(task, odometer), vehicleLabel)
        }

        val active = tasksWithUrgency.filterNot { it.task.completed }
        val completed = tasksWithUrgency.filter { it.task.completed }
            .sortedByDescending { it.task.lastDoneDate ?: it.task.createdAt }

        val hasAnyTasks = allTasks.any { it.vehicleId in activeVehicleIds }

        MaintenanceUiState(
            vehicles = vehicles,
            selectedVehicleId = selectedId,
            overdueTasks = active.filter { it.urgency is TaskUrgency.Overdue },
            dueSoonTasks = active.filter { it.urgency is TaskUrgency.DueSoon },
            upcomingTasks = active.filter { it.urgency is TaskUrgency.Upcoming },
            upToDateTasks = active.filter { it.urgency is TaskUrgency.UpToDate },
            completedTasks = completed,
            showCompleted = showDone,
            hasAnyTasks = hasAnyTasks,
            isLoading = false,
            pendingRecordReview = pendingReview,
            pendingTaskToComplete = pendingTask,
            showUndoCompletionWarning = undoWarning
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MaintenanceUiState())

    fun selectVehicle(vehicleId: String?) {
        selectedVehicleId.value = vehicleId
    }

    fun toggleShowCompleted() {
        showCompleted.value = !showCompleted.value
    }

    fun toggleTaskComplete(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            if (completed) {
                // If completing, prepare a service record for review
                val vehicle = vehicleRepository.getVehicleById(task.vehicleId)
                val newRecord = ServiceRecord(
                    id = java.util.UUID.randomUUID().toString(),
                    vehicleId = task.vehicleId,
                    title = task.name,
                    category = task.category,
                    odometer = vehicle?.odometer ?: 0,
                    date = System.currentTimeMillis()
                )
                pendingTaskToComplete.value = task
                pendingRecordReview.value = newRecord
            } else {
                // If unmarking as done, check if there's an associated record to delete
                if (task.associatedRecordId != null) {
                    showUndoCompletionWarning.value = task
                } else {
                    taskRepository.setCompleted(taskId, false, null)
                }
            }
        }
    }

    fun confirmRecordAndCompleteTask(record: ServiceRecord) {
        val task = pendingTaskToComplete.value ?: return
        viewModelScope.launch {
            serviceRecordRepository.addRecord(record)
            taskRepository.setCompleted(task.id, true, record.id)
            dismissReview()
        }
    }

    fun skipRecordAndCompleteTask() {
        val task = pendingTaskToComplete.value ?: return
        viewModelScope.launch {
            taskRepository.setCompleted(task.id, true, null)
            dismissReview()
        }
    }

    fun dismissReview() {
        pendingRecordReview.value = null
        pendingTaskToComplete.value = null
    }

    fun confirmUndoCompletion() {
        val task = showUndoCompletionWarning.value ?: return
        viewModelScope.launch {
            task.associatedRecordId?.let { recordId ->
                serviceRecordRepository.deleteRecord(recordId)
            }
            taskRepository.setCompleted(task.id, false, null)
            dismissUndoWarning()
        }
    }

    fun dismissUndoWarning() {
        showUndoCompletionWarning.value = null
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }
}
