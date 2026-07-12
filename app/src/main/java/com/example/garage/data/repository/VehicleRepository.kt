package com.example.garage.data.repository

import com.example.garage.data.local.dao.VehicleDao
import com.example.garage.data.local.entity.toDomain
import com.example.garage.data.local.entity.toEntity
import com.example.garage.data.remote.FirestoreVehicleSource
import com.example.garage.domain.model.Vehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepository @Inject constructor(
    private val dao: VehicleDao,
    private val remote: FirestoreVehicleSource,
    private val authRepository: AuthRepository,
    private val serviceRecordRepository: ServiceRecordRepository,
    private val maintenanceTaskRepository: MaintenanceTaskRepository
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun tryPush() {
        authRepository.currentUserId?.let { uid ->
            repositoryScope.launch {
                pushUnsynced(uid)
            }
        }
    }

    fun observeVehicles(includeArchived: Boolean = false): Flow<List<Vehicle>> =
        if (includeArchived) {
            dao.observeArchived().map { list -> list.map { it.toDomain() } }
        } else {
            dao.observeAll().map { list -> list.map { it.toDomain() } }
        }

    fun observeVehicle(id: String): Flow<Vehicle?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun getVehicleById(id: String): Vehicle? =
        dao.getById(id)?.toDomain()

    suspend fun addVehicle(vehicle: Vehicle): Vehicle {
        val withId = if (vehicle.id.isBlank()) vehicle.copy(id = UUID.randomUUID().toString()) else vehicle
        dao.upsert(withId.toEntity(isSynced = false))
        tryPush()
        return withId
    }

    suspend fun updateOdometer(vehicleId: String, odometer: Int) {
        dao.updateOdometer(vehicleId, odometer)
        tryPush()
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        dao.setArchived(id, archived)
        tryPush()
    }

    suspend fun deleteVehicle(id: String) {
        serviceRecordRepository.deleteRecordsForVehicle(id)
        maintenanceTaskRepository.deleteTasksForVehicle(id)
        dao.markDeleted(id)
        tryPush()
    }

    /** Pulls remote changes into Room. Runs for as long as [scope] is alive. */
    fun startRemoteListener(scope: CoroutineScope, uid: String) {
        scope.launch {
            remote.observe(uid)
                .catch { /* offline or permission error - Room stays the source of truth */ }
                .collect { remoteVehicles -> dao.sync(remoteVehicles) }
        }
    }

    /** Pushes any locally-dirty rows up to Firestore. Safe to call repeatedly. */
    suspend fun pushUnsynced(uid: String) {
        dao.getUnsynced().forEach { entity ->
            if (entity.isDeleted) {
                runCatching { remote.delete(uid, entity.id) }
                    .onSuccess { dao.hardDelete(entity.id) }
            } else {
                runCatching { remote.push(uid, entity) }
                    .onSuccess { dao.markSynced(entity.id) }
            }
        }
    }
}
