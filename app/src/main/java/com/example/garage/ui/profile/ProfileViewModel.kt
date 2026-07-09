package com.example.garage.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Minimal for now - just enough to close the sign-in/sign-out loop while
 * the rest of the app is built out in later phases (settings, theme
 * toggle, app version, etc. will land here).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val user: StateFlow<com.example.garage.domain.model.GarageUser?> =
        authRepository.observeAuthState()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun signOut() = authRepository.signOut()
}
