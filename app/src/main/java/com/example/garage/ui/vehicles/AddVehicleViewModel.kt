package com.example.garage.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddVehicleUiState(
    val isEdit: Boolean = false,
    val year: String = "",
    val make: String = "",
    val model: String = "",
    val submodel: String = "",
    val engine: String = "",
    val odometer: String = "",
    val notes: String = "",
    val isArchived: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class AddVehicleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val vehicleId: String? = savedStateHandle["vehicleId"]

    private val _uiState = MutableStateFlow(AddVehicleUiState())
    val uiState: StateFlow<AddVehicleUiState> = _uiState.asStateFlow()

    init {
        vehicleId?.let { id ->
            viewModelScope.launch {
                vehicleRepository.getVehicleById(id)?.let { vehicle ->
                    _uiState.value = AddVehicleUiState(
                        isEdit = true,
                        year = vehicle.year,
                        make = vehicle.make,
                        model = vehicle.model,
                        submodel = vehicle.submodel ?: "",
                        engine = vehicle.engine ?: "",
                        odometer = vehicle.odometer.toString(),
                        notes = vehicle.notes ?: "",
                        isArchived = vehicle.isArchived
                    )
                }
            }
        }
    }

    fun onYearChange(value: String) { _uiState.value = _uiState.value.copy(year = value, error = null) }
    fun onMakeChange(value: String) { _uiState.value = _uiState.value.copy(make = value, error = null) }
    fun onModelChange(value: String) { _uiState.value = _uiState.value.copy(model = value, error = null) }
    fun onSubmodelChange(value: String) { _uiState.value = _uiState.value.copy(submodel = value) }
    fun onEngineChange(value: String) { _uiState.value = _uiState.value.copy(engine = value) }
    fun onOdometerChange(value: String) {
        if (value.all { it.isDigit() }) _uiState.value = _uiState.value.copy(odometer = value, error = null)
    }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }

    fun save() {
        val state = _uiState.value
        if (state.year.isBlank() || state.make.isBlank() || state.model.isBlank()) {
            _uiState.value = state.copy(error = "Year, make, and model are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            runCatching {
                vehicleRepository.addVehicle(
                    Vehicle(
                        id = vehicleId ?: "",
                        year = state.year.trim(),
                        make = state.make.trim(),
                        model = state.model.trim(),
                        submodel = state.submodel.trim().ifBlank { null },
                        engine = state.engine.trim().ifBlank { null },
                        notes = state.notes.trim().ifBlank { null },
                        odometer = state.odometer.toIntOrNull() ?: 0,
                        isArchived = state.isArchived
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "Couldn't save this vehicle")
            }
        }
    }
}
