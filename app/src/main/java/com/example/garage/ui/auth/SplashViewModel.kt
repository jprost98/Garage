package com.example.garage.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SplashUiState {
    object Loading : SplashUiState
    object Authenticated : SplashUiState
    object Unauthenticated : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<SplashUiState> = authRepository.observeAuthState()
        .map { user ->
            // Minimal delay to ensure a smooth transition and visual stability
            delay(1000)
            if (user != null) {
                SplashUiState.Authenticated
            } else {
                SplashUiState.Unauthenticated
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SplashUiState.Loading
        )
}
