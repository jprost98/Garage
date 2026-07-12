package com.example.garage.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.domain.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleDetailUiState(
    val vehicle: Vehicle? = null,
    val records: List<ServiceRecord> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    serviceRecordRepository: ServiceRecordRepository
) : ViewModel() {

    val vehicleId: String = checkNotNull(savedStateHandle["vehicleId"])

    val uiState: StateFlow<VehicleDetailUiState> = combine(
        vehicleRepository.observeVehicle(vehicleId),
        serviceRecordRepository.observeForVehicle(vehicleId)
    ) { vehicle, records ->
        VehicleDetailUiState(vehicle = vehicle, records = records, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VehicleDetailUiState())

    fun deleteVehicle() {
        viewModelScope.launch {
            vehicleRepository.deleteVehicle(vehicleId)
        }
    }

    fun archiveVehicle() {
        viewModelScope.launch {
            vehicleRepository.setArchived(vehicleId, true)
        }
    }
}
