package com.example.garage.data.repository

import com.example.garage.data.local.dao.MaintenanceTaskDao
import com.example.garage.data.local.entity.toDomain
import com.example.garage.data.local.entity.toEntity
import com.example.garage.data.remote.FirestoreMaintenanceTaskSource
import com.example.garage.domain.model.MaintenanceTask
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
class MaintenanceTaskRepository @Inject constructor(
    private val dao: MaintenanceTaskDao,
    private val remote: FirestoreMaintenanceTaskSource,
    private val authRepository: AuthRepository
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun tryPush() {
        authRepository.currentUserId?.let { uid ->
            repositoryScope.launch {
                pushUnsynced(uid)
            }
        }
    }

    fun observeAll(): Flow<List<MaintenanceTask>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeForVehicle(vehicleId: String): Flow<List<MaintenanceTask>> =
        dao.observeForVehicle(vehicleId).map { list -> list.map { it.toDomain() } }

    suspend fun addTask(task: MaintenanceTask): MaintenanceTask {
        val withId = if (task.id.isBlank()) task.copy(id = UUID.randomUUID().toString()) else task
        dao.upsert(withId.toEntity(isSynced = false))
        tryPush()
        return withId
    }

    suspend fun setCompleted(id: String, completed: Boolean) {
        dao.setCompleted(id, completed)
        tryPush()
    }

    suspend fun deleteTask(id: String) {
        dao.markDeleted(id)
        tryPush()
    }

    suspend fun deleteTasksForVehicle(vehicleId: String) {
        dao.markDeletedForVehicle(vehicleId)
        tryPush()
    }

    suspend fun getActiveTasksOnce(): List<MaintenanceTask> =
        dao.getActiveTasks().map { it.toDomain() }

    fun startRemoteListener(scope: CoroutineScope, uid: String) {
        scope.launch {
            remote.observe(uid)
                .catch { }
                .collect { remoteTasks -> dao.sync(remoteTasks) }
        }
    }

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
