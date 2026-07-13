package com.example.garage.ui.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.ServiceCategory
import com.example.garage.domain.model.TaskType
import com.example.garage.domain.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddMaintenanceTaskUiState(
    val isEdit: Boolean = false,
    val vehicleId: String = "",
    val vehicles: List<Vehicle> = emptyList(),
    val name: String = "",
    val category: ServiceCategory = ServiceCategory.OTHER,
    val type: TaskType = TaskType.RECURRING,
    val intervalMiles: String = "",
    val intervalMonths: String = "",
    val startingOdometer: String = "",
    val lastDoneDate: Long? = null,
    val lastDoneOdometer: String = "",
    val dueDate: Long? = null,
    val dueOdometer: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class AddMaintenanceTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MaintenanceTaskRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val initialVehicleId: String? = savedStateHandle["vehicleId"]
    private val taskId: String? = savedStateHandle["taskId"]

    private val _uiState = MutableStateFlow(AddMaintenanceTaskUiState())
    val uiState: StateFlow<AddMaintenanceTaskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val vehicles = vehicleRepository.observeVehicles().first()
            _uiState.value = _uiState.value.copy(
                vehicles = vehicles,
                vehicleId = initialVehicleId ?: vehicles.firstOrNull()?.id ?: ""
            )

            taskId?.let { id ->
                repository.getTaskById(id)?.let { task ->
                    _uiState.value = _uiState.value.copy(
                        isEdit = true,
                        vehicleId = task.vehicleId,
                        name = task.name,
                        category = task.category,
                        type = task.type,
                        intervalMiles = task.intervalMiles?.toString() ?: "",
                        intervalMonths = task.intervalMonths?.toString() ?: "",
                        startingOdometer = task.startingOdometer?.toString() ?: "",
                        lastDoneDate = task.lastDoneDate,
                        lastDoneOdometer = task.lastDoneOdometer?.toString() ?: "",
                        dueDate = task.dueDate,
                        dueOdometer = task.dueOdometer?.toString() ?: "",
                        notes = task.notes ?: ""
                    )
                }
            }
        }
    }

    fun onVehicleChange(value: String) { _uiState.value = _uiState.value.copy(vehicleId = value) }
    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value, error = null) }
    fun onCategoryChange(value: ServiceCategory) { _uiState.value = _uiState.value.copy(category = value) }
    fun onTypeChange(value: TaskType) { _uiState.value = _uiState.value.copy(type = value) }
    fun onIntervalMilesChange(value: String) {
        if (value.all { it.isDigit() }) _uiState.value = _uiState.value.copy(intervalMiles = value)
    }
    fun onIntervalMonthsChange(value: String) {
        if (value.all { it.isDigit() }) _uiState.value = _uiState.value.copy(intervalMonths = value)
    }
    fun onStartingOdometerChange(value: String) {
        if (value.all { it.isDigit() }) _uiState.value = _uiState.value.copy(startingOdometer = value)
    }
    fun onLastDoneDateChange(value: Long?) { _uiState.value = _uiState.value.copy(lastDoneDate = value) }
    fun onLastDoneOdometerChange(value: String) {
        if (value.all { it.isDigit() }) _uiState.value = _uiState.value.copy(lastDoneOdometer = value)
    }
    fun onDueDateChange(value: Long?) { _uiState.value = _uiState.value.copy(dueDate = value) }
    fun onDueOdometerChange(value: String) {
        if (value.all { it.isDigit() }) _uiState.value = _uiState.value.copy(dueOdometer = value)
    }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }

    fun save() {
        val state = _uiState.value
        if (state.vehicleId.isBlank()) {
            _uiState.value = state.copy(error = "Please select a vehicle")
            return
        }
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Task name is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            runCatching {
                val interval = state.intervalMiles.toIntOrNull()
                var calculatedDueOdometer = state.dueOdometer.toIntOrNull()

                if (state.type == TaskType.RECURRING && interval != null && calculatedDueOdometer == null) {
                    val vehicle = vehicleRepository.getVehicleById(state.vehicleId)
                    val currentOdo = vehicle?.odometer ?: 0
                    val startOdo = state.startingOdometer.toIntOrNull() ?: 0

                    if (currentOdo >= startOdo) {
                        val offset = currentOdo - startOdo
                        calculatedDueOdometer = startOdo + ((offset / interval) + 1) * interval
                    } else {
                        calculatedDueOdometer = startOdo + interval
                    }
                }

                val task = MaintenanceTask(
                    id = taskId ?: UUID.randomUUID().toString(),
                    vehicleId = state.vehicleId,
                    name = state.name.trim(),
                    type = state.type,
                    category = state.category,
                    notes = state.notes.trim().ifBlank { null },
                    intervalMiles = interval,
                    intervalMonths = state.intervalMonths.toIntOrNull(),
                    startingOdometer = state.startingOdometer.toIntOrNull(),
                    lastDoneDate = state.lastDoneDate,
                    lastDoneOdometer = state.lastDoneOdometer.toIntOrNull(),
                    dueDate = state.dueDate,
                    dueOdometer = calculatedDueOdometer,
                    createdAt = System.currentTimeMillis()
                )
                repository.addTask(task)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "Couldn't save task")
            }
        }
    }

    fun delete() {
        taskId?.let { id ->
            viewModelScope.launch {
                repository.deleteTask(id)
                _uiState.value = _uiState.value.copy(saved = true)
            }
        }
    }
}
