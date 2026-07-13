package com.example.garage.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.Vehicle
import com.google.firebase.ai.TemplateGenerativeModel
import com.google.firebase.ai.type.PublicPreviewAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantUiState(
    val prompt: String = "",
    val response: String = "",
    val isLoading: Boolean = false,
    val vehicles: List<Vehicle> = emptyList()
)

@HiltViewModel
@OptIn(PublicPreviewAPI::class)
class AssistantViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val model: TemplateGenerativeModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState = _uiState.asStateFlow()

    private var conversationHistory = ""

    init {
        loadVehicles()
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            vehicleRepository.observeVehicles().collect { vehicles ->
                _uiState.value = _uiState.value.copy(vehicles = vehicles)
            }
        }
    }

    fun onPromptChange(newPrompt: String) {
        _uiState.value = _uiState.value.copy(prompt = newPrompt)
    }

    fun askAssistant() {
        val prompt = _uiState.value.prompt
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val vehiclesInfo = _uiState.value.vehicles.joinToString { "${it.year} ${it.make} ${it.model} (Odometer: ${it.odometer} mi)" }

            try {
                val result = model.generateContent(
                    "garage-assistant",
                    mapOf(
                        "vehicleContext" to vehiclesInfo.ifBlank { "No vehicles saved yet." },
                        "conversationHistory" to conversationHistory.ifBlank { "None" },
                        "userPrompt" to prompt
                    )
                )
                val responseText = result.text ?: "I'm sorry, I couldn't generate a response."
                
                // Append this interaction to the history for subsequent questions
                conversationHistory += "User: $prompt\nAssistant: $responseText\n\n"

                _uiState.value = _uiState.value.copy(
                    response = responseText,
                    isLoading = false,
                    prompt = "" // Clear the prompt after success
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    response = "Error communicating with AI: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
}
