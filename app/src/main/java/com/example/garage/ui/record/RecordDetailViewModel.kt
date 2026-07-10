package com.example.garage.ui.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.domain.model.ServiceRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordDetailUiState(
    val record: ServiceRecord? = null,
    val isLoading: Boolean = true,
    val deleted: Boolean = false
)

@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serviceRecordRepository: ServiceRecordRepository
) : ViewModel() {

    private val recordId: String = checkNotNull(savedStateHandle["recordId"])
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<RecordDetailUiState> = combine(
        serviceRecordRepository.observeById(recordId),
        deleted
    ) { record, isDeleted ->
        RecordDetailUiState(record = record, isLoading = false, deleted = isDeleted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordDetailUiState())

    fun delete() {
        viewModelScope.launch {
            serviceRecordRepository.deleteRecord(recordId)
            deleted.value = true
        }
    }
}
