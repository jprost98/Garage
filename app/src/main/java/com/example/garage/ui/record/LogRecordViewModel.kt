package com.example.garage.ui.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.domain.model.ServiceCategory
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.domain.usecase.ParseServiceEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogRecordUiState(
    val freeText: String = "",
    val title: String = "",
    val category: ServiceCategory? = null,
    val odometer: String = "",
    val cost: String = "",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class LogRecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serviceRecordRepository: ServiceRecordRepository,
    private val parseServiceEntry: ParseServiceEntryUseCase
) : ViewModel() {

    private val vehicleId: String = checkNotNull(savedStateHandle["vehicleId"])

    private val _uiState = MutableStateFlow(LogRecordUiState())
    val uiState: StateFlow<LogRecordUiState> = _uiState.asStateFlow()

    /**
     * Re-parses on every keystroke and overwrites title/category/odometer
     * with whatever it guesses - a v1 simplification. If someone types a
     * sentence, then manually fixes the category chip, then keeps typing
     * in the free-text field, their manual fix gets overwritten again.
     * Good enough for now; a v2 could only apply a guess the first time a
     * field goes from empty to non-empty.
     */
    fun onFreeTextChange(value: String) {
        val parsed = parseServiceEntry(value)
        _uiState.value = _uiState.value.copy(
            freeText = value,
            title = parsed.title,
            category = parsed.category ?: _uiState.value.category,
            odometer = parsed.odometer?.toString() ?: _uiState.value.odometer,
            error = null
        )
    }

    fun onTitleChange(value: String) { _uiState.value = _uiState.value.copy(title = value, error = null) }
    fun onCategorySelect(category: ServiceCategory) { _uiState.value = _uiState.value.copy(category = category) }
    fun onOdometerChange(value: String) {
        if (value.all { it.isDigit() }) _uiState.value = _uiState.value.copy(odometer = value, error = null)
    }
    fun onCostChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
            _uiState.value = _uiState.value.copy(cost = value)
        }
    }
    fun onDescriptionChange(value: String) { _uiState.value = _uiState.value.copy(description = value) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(error = "Give this record a title")
            return
        }
        if (state.odometer.isBlank()) {
            _uiState.value = state.copy(error = "Odometer reading is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            runCatching {
                serviceRecordRepository.addRecord(
                    ServiceRecord(
                        id = "",
                        vehicleId = vehicleId,
                        title = state.title.trim(),
                        category = state.category ?: ServiceCategory.OTHER,
                        description = state.description.trim().ifBlank { null },
                        odometer = state.odometer.toIntOrNull() ?: 0,
                        cost = state.cost.toDoubleOrNull(),
                        date = state.date
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "Couldn't save this record")
            }
        }
    }
}
