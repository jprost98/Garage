package com.example.garage.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleListItem(val vehicle: Vehicle, val openTaskCount: Int)

@HiltViewModel
class VehiclesViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val taskRepository: MaintenanceTaskRepository
) : ViewModel() {

    val vehicles: StateFlow<List<VehicleListItem>> = combine(
        vehicleRepository.observeVehicles(),
        taskRepository.observeAll()
    ) { vehicles, tasks ->
        vehicles.map { vehicle ->
            VehicleListItem(
                vehicle = vehicle,
                openTaskCount = tasks.count { it.vehicleId == vehicle.id && !it.completed }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteVehicle(id: String) {
        viewModelScope.launch { vehicleRepository.deleteVehicle(id) }
    }
}
