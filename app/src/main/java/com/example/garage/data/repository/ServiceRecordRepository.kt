package com.example.garage.data.repository

import com.example.garage.data.local.dao.ServiceRecordDao
import com.example.garage.data.local.entity.toDomain
import com.example.garage.data.local.entity.toEntity
import com.example.garage.data.remote.FirestoreServiceRecordSource
import com.example.garage.domain.model.ServiceRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceRecordRepository @Inject constructor(
    private val dao: ServiceRecordDao,
    private val remote: FirestoreServiceRecordSource
) {
    fun observeAll(): Flow<List<ServiceRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeForVehicle(vehicleId: String): Flow<List<ServiceRecord>> =
        dao.observeForVehicle(vehicleId).map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<ServiceRecord?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun addRecord(record: ServiceRecord): ServiceRecord {
        val withId = if (record.id.isBlank()) record.copy(id = UUID.randomUUID().toString()) else record
        dao.upsert(withId.toEntity(isSynced = false))
        return withId
    }

    suspend fun deleteRecord(id: String) {
        dao.markDeleted(id)
    }

    fun startRemoteListener(scope: CoroutineScope, uid: String) {
        scope.launch {
            remote.observe(uid)
                .catch { }
                .collect { remoteRecords -> dao.upsertAll(remoteRecords) }
        }
    }

    suspend fun pushUnsynced(uid: String) {
        dao.getUnsynced().forEach { entity ->
            runCatching { remote.push(uid, entity) }
                .onSuccess { dao.markSynced(entity.id) }
        }
    }
}
