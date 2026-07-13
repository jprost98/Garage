package com.example.garage.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.AuthRepository
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.data.repository.UserRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.GarageUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles user profile display and updates. Profile data is stored
 * in Room and synced to Firestore via SyncCoordinator.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val vehicleRepository: VehicleRepository,
    private val serviceRecordRepository: ServiceRecordRepository
) : ViewModel() {

    val authState: StateFlow<GarageUser?> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser)

    val user: StateFlow<GarageUser?> = authState
        .flatMapLatest { authUser ->
            if (authUser != null) {
                userRepository.observeUser(authUser.uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser)

    val vehicleCount: StateFlow<Int> = vehicleRepository.observeVehicles()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val serviceRecordCount: StateFlow<Int> = serviceRecordRepository.observeAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun updateProfile(
        firstName: String,
        lastName: String,
        email: String,
        birthday: Long?
    ) {
        val authUser = authState.value ?: return
        viewModelScope.launch {
            var emailToSave = authUser.email
            if (email != authUser.email && email.isNotEmpty()) {
                authRepository.updateEmail(email)
                    .onSuccess { emailToSave = email }
                    .onFailure {
                        // In a real app, show a "Re-authentication required" message
                    }
            }

            val updatedUser = GarageUser(
                uid = authUser.uid,
                firstName = firstName,
                lastName = lastName,
                email = emailToSave,
                displayName = "$firstName $lastName".trim().ifEmpty { authUser.displayName },
                birthday = birthday
            )
            userRepository.saveUserLocally(updatedUser)
            userRepository.pushUnsynced()
        }
    }

    fun signOut() = authRepository.signOut()
}