package com.example.garage.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.domain.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleListItem(
    val vehicle: Vehicle,
    val openTaskCount: Int,
    val recordCount: Int
)

@HiltViewModel
class VehiclesViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val taskRepository: MaintenanceTaskRepository,
    private val serviceRecordRepository: ServiceRecordRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    val vehicles: StateFlow<List<VehicleListItem>> = combine(
        vehicleRepository.observeVehicles(),
        taskRepository.observeAll(),
        serviceRecordRepository.observeAll()
    ) { vehicles, tasks, records ->
        vehicles.map { vehicle ->
            VehicleListItem(
                vehicle = vehicle,
                openTaskCount = tasks.count { it.vehicleId == vehicle.id && !it.completed },
                recordCount = records.count { it.vehicleId == vehicle.id }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<ServiceRecord>> = combine(
        searchQuery,
        serviceRecordRepository.observeAll()
    ) { query, records ->
        if (query.isBlank()) {
            emptyList()
        } else {
            records.filter { record ->
                record.title.contains(query, ignoreCase = true) ||
                        record.description?.contains(query, ignoreCase = true) == true ||
                        record.category.label.contains(query, ignoreCase = true)
            }.sortedByDescending { it.date }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun deleteVehicle(id: String) {
        viewModelScope.launch { vehicleRepository.deleteVehicle(id) }
    }
}
