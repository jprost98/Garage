package com.example.garage.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.TaskUrgency
import com.example.garage.domain.usecase.TaskUrgencyCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    vehicleRepository: VehicleRepository,
    serviceRecordRepository: ServiceRecordRepository,
    maintenanceTaskRepository: MaintenanceTaskRepository,
    private val urgencyCalculator: TaskUrgencyCalculator
) : ViewModel() {

    private val selectedVehicleId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        vehicleRepository.observeVehicles(),
        serviceRecordRepository.observeAll(),
        maintenanceTaskRepository.observeAll(),
        selectedVehicleId
    ) { vehicles, records, tasks, selectedId ->
        val effectiveSelectedId = selectedId ?: vehicles.firstOrNull()?.id
        val selectedVehicle = vehicles.firstOrNull { it.id == effectiveSelectedId }
        val currentOdometer = selectedVehicle?.odometer ?: 0

        val relevantTasks = tasks.filter { !it.completed && (effectiveSelectedId == null || it.vehicleId == effectiveSelectedId) }
        val dueTasks = relevantTasks
            .map { TaskWithUrgency(it, urgencyCalculator.urgencyFor(it, currentOdometer)) }
            .filter { it.urgency is TaskUrgency.Overdue || it.urgency is TaskUrgency.DueSoon }
            .sortedBy { if (it.urgency is TaskUrgency.Overdue) 0 else 1 }

        val recentRecords = records
            .filter { effectiveSelectedId == null || it.vehicleId == effectiveSelectedId }
            .take(5)

        HomeUiState(
            vehicles = vehicles,
            selectedVehicleId = effectiveSelectedId,
            dueTasks = dueTasks,
            recentRecords = recentRecords,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun selectVehicle(vehicleId: String) {
        selectedVehicleId.update { vehicleId }
    }
}
