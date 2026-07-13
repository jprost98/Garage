package com.example.garage.ui.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.TaskUrgency
import com.example.garage.domain.usecase.TaskUrgencyCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: MaintenanceTask? = null,
    val urgency: TaskUrgency = TaskUrgency.UpToDate,
    val isLoading: Boolean = true,
    val deleted: Boolean = false
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MaintenanceTaskRepository,
    private val vehicleRepository: VehicleRepository,
    private val urgencyCalculator: TaskUrgencyCalculator
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                val vehicle = vehicleRepository.getVehicleById(task.vehicleId)
                val urgency = urgencyCalculator.urgencyFor(task, vehicle?.odometer ?: 0)
                _uiState.value = TaskDetailUiState(task = task, urgency = urgency, isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleComplete() {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            repository.setCompleted(task.id, !task.completed)
            loadTask()
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            _uiState.value = _uiState.value.copy(deleted = true)
        }
    }
}
