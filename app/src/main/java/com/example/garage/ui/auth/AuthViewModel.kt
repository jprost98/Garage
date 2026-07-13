package com.example.garage.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignedIn: Boolean = false,
    val infoMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isSignedIn = authRepository.currentUser != null))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) { 
        _uiState.value = _uiState.value.copy(email = value, error = null, infoMessage = null) 
    }
    fun onPasswordChange(value: String) { 
        _uiState.value = _uiState.value.copy(password = value, error = null, infoMessage = null) 
    }

    fun signIn() = submit { authRepository.signIn(_uiState.value.email.trim(), _uiState.value.password) }

    fun register() = submit { authRepository.register(_uiState.value.email.trim(), _uiState.value.password) }

    fun resetPassword() {
        val email = _uiState.value.email.trim()
        if (email.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your email to reset your password")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, infoMessage = null)
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        infoMessage = "Password reset email sent to $email"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to send password reset email"
                    )
                }
        }
    }

    private fun submit(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, infoMessage = null)
            action()
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, isSignedIn = true) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Something went wrong") }
        }
    }
}
