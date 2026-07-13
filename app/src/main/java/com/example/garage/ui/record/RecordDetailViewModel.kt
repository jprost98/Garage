package com.example.garage.ui.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.domain.model.MaintenanceTask
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
    val deleted: Boolean = false,
    val linkedTask: MaintenanceTask? = null,
    val showDeleteConfirmation: Boolean = false,
    val showDeleteReceiptConfirmation: Boolean = false
)

@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serviceRecordRepository: ServiceRecordRepository,
    private val taskRepository: MaintenanceTaskRepository
) : ViewModel() {

    private val recordId: String = checkNotNull(savedStateHandle["recordId"])
    private val deleted = MutableStateFlow(false)
    private val showDeleteConfirmation = MutableStateFlow(false)
    private val showDeleteReceiptConfirmation = MutableStateFlow(false)
    private val linkedTask = MutableStateFlow<MaintenanceTask?>(null)

    init {
        viewModelScope.launch {
            linkedTask.value = taskRepository.getTaskByAssociatedRecordId(recordId)
        }
    }

    val uiState: StateFlow<RecordDetailUiState> = combine(
        serviceRecordRepository.observeById(recordId),
        deleted,
        showDeleteConfirmation,
        linkedTask,
        showDeleteReceiptConfirmation
    ) { params: Array<Any?> ->
        RecordDetailUiState(
            record = params[0] as ServiceRecord?,
            deleted = params[1] as Boolean,
            showDeleteConfirmation = params[2] as Boolean,
            linkedTask = params[3] as MaintenanceTask?,
            showDeleteReceiptConfirmation = params[4] as Boolean,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordDetailUiState())

    fun onDeleteClick() {
        showDeleteConfirmation.value = true
    }

    fun cancelDelete() {
        showDeleteConfirmation.value = false
    }

    fun confirmDelete(uncompleteTask: Boolean) {
        viewModelScope.launch {
            if (uncompleteTask) {
                linkedTask.value?.let { task ->
                    taskRepository.setCompleted(task.id, false, null)
                }
            }
            serviceRecordRepository.deleteRecord(recordId)
            showDeleteConfirmation.value = false
            deleted.value = true
        }
    }

    fun onDeleteReceiptClick() {
        showDeleteReceiptConfirmation.value = true
    }

    fun cancelDeleteReceipt() {
        showDeleteReceiptConfirmation.value = false
    }

    fun confirmDeleteReceipt() {
        viewModelScope.launch {
            val currentRecord = uiState.value.record ?: return@launch
            serviceRecordRepository.addRecord(currentRecord.copy(receiptPhotoUrl = null))
            showDeleteReceiptConfirmation.value = false
        }
    }
}
