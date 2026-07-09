package com.example.garage.data.sync

import com.example.garage.data.repository.AuthRepository
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.data.repository.VehicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wires local <-> remote sync together for the whole app. Call [start]
 * once, from GarageApplication, with a process-lifetime CoroutineScope.
 *
 * When a user signs in: start a Firestore listener per collection (pulls
 * remote changes into Room) and push anything created locally while
 * offline. When they sign out: cancel the listeners so the next user's
 * data doesn't get mixed in.
 */
@Singleton
class SyncCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val vehicleRepository: VehicleRepository,
    private val serviceRecordRepository: ServiceRecordRepository,
    private val maintenanceTaskRepository: MaintenanceTaskRepository
) {
    private var syncJob: Job? = null

    fun start(appScope: CoroutineScope) {
        appScope.launch {
            authRepository.observeAuthState().collect { user ->
                syncJob?.cancel()
                if (user != null) {
                    syncJob = appScope.launch {
                        vehicleRepository.startRemoteListener(this, user.uid)
                        serviceRecordRepository.startRemoteListener(this, user.uid)
                        maintenanceTaskRepository.startRemoteListener(this, user.uid)
                        vehicleRepository.pushUnsynced(user.uid)
                        serviceRecordRepository.pushUnsynced(user.uid)
                        maintenanceTaskRepository.pushUnsynced(user.uid)
                    }
                }
            }
        }
    }
}
