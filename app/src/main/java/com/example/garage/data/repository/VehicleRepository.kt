package com.example.garage.data.repository

import com.example.garage.data.local.dao.VehicleDao
import com.example.garage.data.local.entity.toDomain
import com.example.garage.data.local.entity.toEntity
import com.example.garage.data.remote.FirestoreVehicleSource
import com.example.garage.domain.model.Vehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first repository: every read comes from Room, every write goes
 * to Room first (so the UI updates instantly, online or not), then a
 * best-effort push to Firestore happens in the background. If the push
 * fails, the row just stays marked unsynced until the next sync pass -
 * see [pushUnsynced] and [SyncCoordinator].
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val dao: VehicleDao,
    private val remote: FirestoreVehicleSource
) {
    fun observeVehicles(): Flow<List<Vehicle>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeVehicle(id: String): Flow<Vehicle?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun addVehicle(vehicle: Vehicle): Vehicle {
        val withId = if (vehicle.id.isBlank()) vehicle.copy(id = UUID.randomUUID().toString()) else vehicle
        dao.upsert(withId.toEntity(isSynced = false))
        return withId
    }

    suspend fun updateOdometer(vehicleId: String, odometer: Int) {
        dao.updateOdometer(vehicleId, odometer)
    }

    suspend fun deleteVehicle(id: String) {
        dao.markDeleted(id)
    }

    /** Pulls remote changes into Room. Runs for as long as [scope] is alive. */
    fun startRemoteListener(scope: CoroutineScope, uid: String) {
        scope.launch {
            remote.observe(uid)
                .catch { /* offline or permission error - Room stays the source of truth */ }
                .collect { remoteVehicles -> dao.upsertAll(remoteVehicles) }
        }
    }

    /** Pushes any locally-dirty rows up to Firestore. Safe to call repeatedly. */
    suspend fun pushUnsynced(uid: String) {
        dao.getUnsynced().forEach { entity ->
            runCatching { remote.push(uid, entity) }
                .onSuccess { dao.markSynced(entity.id) }
        }
    }
}
