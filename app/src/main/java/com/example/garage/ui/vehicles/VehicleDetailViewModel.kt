package com.example.garage.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.remote.MaintenanceAdvisor
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.domain.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleDetailUiState(
    val vehicle: Vehicle? = null,
    val records: List<ServiceRecord> = emptyList(),
    val sortOption: RecordSortOption = RecordSortOption.DATE_DESC,
    val isLoading: Boolean = true,
    val isSuggesting: Boolean = false,
    val suggestions: List<MaintenanceTask> = emptyList(),
    val selectedSuggestionNames: Set<String> = emptySet(),
    val suggestionResponse: String? = null
)

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val maintenanceTaskRepository: MaintenanceTaskRepository,
    private val serviceRecordRepository: ServiceRecordRepository,
    private val maintenanceAdvisor: MaintenanceAdvisor
) : ViewModel() {

    val vehicleId: String = checkNotNull(savedStateHandle["vehicleId"])
    private val sortOption = MutableStateFlow(RecordSortOption.DATE_DESC)
    private val isSuggesting = MutableStateFlow(false)
    private val suggestions = MutableStateFlow<List<MaintenanceTask>>(emptyList())
    private val selectedSuggestionNames = MutableStateFlow<Set<String>>(emptySet())
    private val suggestionResponse = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VehicleDetailUiState> = combine(
        vehicleRepository.observeVehicle(vehicleId),
        serviceRecordRepository.observeForVehicle(vehicleId),
        sortOption,
        isSuggesting,
        suggestions,
        selectedSuggestionNames,
        suggestionResponse
    ) { flowArray ->
        val vehicle = flowArray[0] as Vehicle?
        val records = flowArray[1] as List<ServiceRecord>
        val sort = flowArray[2] as RecordSortOption
        val suggesting = flowArray[3] as Boolean
        val sugs = flowArray[4] as List<MaintenanceTask>
        val selected = flowArray[5] as Set<String>
        val responseText = flowArray[6] as String?

        val sortedRecords = when (sort) {
            RecordSortOption.DATE_DESC -> records.sortedByDescending { it.date }
            RecordSortOption.DATE_ASC -> records.sortedBy { it.date }
            RecordSortOption.ODOMETER_DESC -> records.sortedByDescending { it.odometer }
            RecordSortOption.ODOMETER_ASC -> records.sortedBy { it.odometer }
            RecordSortOption.COST_DESC -> records.sortedByDescending { it.cost ?: 0.0 }
            RecordSortOption.COST_ASC -> records.sortedBy { it.cost ?: 0.0 }
        }
        VehicleDetailUiState(
            vehicle = vehicle,
            records = sortedRecords,
            sortOption = sort,
            isLoading = false,
            isSuggesting = suggesting,
            suggestions = sugs,
            selectedSuggestionNames = selected,
            suggestionResponse = responseText
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VehicleDetailUiState())

    fun updateSortOption(option: RecordSortOption) {
        sortOption.value = option
    }

    fun suggestTasks() {
        val vehicle = uiState.value.vehicle ?: return
        viewModelScope.launch {
            isSuggesting.value = true
            
            val existingTasks = maintenanceTaskRepository.getTasksForVehicle(vehicleId)
            val recentRecords = serviceRecordRepository.getRecordsForVehicle(vehicleId)

            val results = maintenanceAdvisor.suggestTasks(
                year = vehicle.year,
                make = vehicle.make,
                modelName = vehicle.model,
                existingTasks = existingTasks,
                recentRecords = recentRecords
            )
            
            suggestions.value = results.tasks
            suggestionResponse.value = results.explanation
            selectedSuggestionNames.value = results.tasks.map { it.name }.toSet()
            isSuggesting.value = false
        }
    }

    fun toggleSuggestion(task: MaintenanceTask) {
        val current = selectedSuggestionNames.value
        selectedSuggestionNames.value = if (current.contains(task.name)) {
            current - task.name
        } else {
            current + task.name
        }
    }

    fun addSelectedSuggestions() {
        val sugsToAdd = suggestions.value.filter { selectedSuggestionNames.value.contains(it.name) }
        viewModelScope.launch {
            val vehicle = uiState.value.vehicle ?: return@launch
            val currentOdo = vehicle.odometer

            sugsToAdd.forEach { task ->
                // Ensure startingOdometer is at least 0, as requested
                val startOdo = task.startingOdometer ?: 0
                val interval = task.intervalMiles
                
                var calculatedDueOdometer = task.dueOdometer
                if (calculatedDueOdometer == null && interval != null && interval > 0) {
                    if (currentOdo >= startOdo) {
                        val offset = currentOdo - startOdo
                        calculatedDueOdometer = startOdo + ((offset / interval) + 1) * interval
                    } else {
                        calculatedDueOdometer = startOdo + interval
                    }
                }

                maintenanceTaskRepository.addTask(
                    task.copy(
                        vehicleId = vehicleId,
                        startingOdometer = startOdo,
                        dueOdometer = calculatedDueOdometer
                    )
                )
            }
            // Clear suggestions after adding
            suggestions.value = emptyList()
            selectedSuggestionNames.value = emptySet()
        }
    }

    fun dismissSuggestions() {
        suggestions.value = emptyList()
        selectedSuggestionNames.value = emptySet()
        suggestionResponse.value = null
    }

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
