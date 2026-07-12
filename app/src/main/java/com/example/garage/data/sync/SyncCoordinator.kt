package com.example.garage.data.sync

import com.example.garage.data.local.AppDatabase
import com.example.garage.data.repository.AuthRepository
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.data.repository.UserRepository
import com.example.garage.data.repository.VehicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wires local <-> remote sync together for the whole app. Call [start]
 * once, from GarageApplication, with a process-lifetime CoroutineScope.
 *
 * When a user signs in: start a Firestore listener per collection (pulls
 * remote changes into Room) and push anything created locally while
 * offline. When they sign out - or a *different* account signs in on the
 * same device - the local Room cache is wiped first. Room has no
 * per-user scoping of its own (it's a single shared set of tables), so
 * without this, the next signed-in user's Firestore listener would just
 * add their rows alongside the previous user's still-present rows and
 * both accounts' data would show up mixed together in the UI.
 */
@Singleton
class SyncCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val vehicleRepository: VehicleRepository,
    private val serviceRecordRepository: ServiceRecordRepository,
    private val maintenanceTaskRepository: MaintenanceTaskRepository,
    private val appDatabase: AppDatabase
) {
    private var syncJob: Job? = null
    private var currentUid: String? = null

    fun start(appScope: CoroutineScope) {
        appScope.launch {
            authRepository.observeAuthState().collect { user ->
                syncJob?.cancelAndJoin()

                if (user?.uid != currentUid) {
                    // Signed out, or a different account signed in - the
                    // cached rows belong to someone else now.
                    withContext(Dispatchers.IO) { appDatabase.clearAllTables() }
                }
                currentUid = user?.uid

                if (user != null) {
                    syncJob = appScope.launch {
                        userRepository.fetchRemoteUser(user.uid)
                        userRepository.pushUnsynced()

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
