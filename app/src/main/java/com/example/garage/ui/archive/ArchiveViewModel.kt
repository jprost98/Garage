package com.example.garage.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.ui.vehicles.VehicleListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val archivedVehicles: List<VehicleListItem> = emptyList(),
    val archivedRecords: List<ServiceRecord> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val serviceRecordRepository: ServiceRecordRepository
) : ViewModel() {

    val uiState: StateFlow<ArchiveUiState> = combine(
        vehicleRepository.observeVehicles(includeArchived = true),
        serviceRecordRepository.observeAll(includeArchived = true)
    ) { archivedVehicles, archivedRecords ->
        ArchiveUiState(
            archivedVehicles = archivedVehicles.map { vehicle ->
                VehicleListItem(
                    vehicle = vehicle,
                    openTaskCount = 0, // Archived vehicles shouldn't have active tasks
                    recordCount = archivedRecords.count { it.vehicleId == vehicle.id }
                )
            },
            archivedRecords = archivedRecords,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchiveUiState())

    fun unarchiveVehicle(id: String) {
        viewModelScope.launch {
            vehicleRepository.setArchived(id, false)
        }
    }
}
