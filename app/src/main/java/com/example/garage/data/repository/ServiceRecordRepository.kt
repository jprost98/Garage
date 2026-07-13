package com.example.garage.data.repository

import android.graphics.Bitmap
import com.example.garage.data.local.dao.ServiceRecordDao
import com.example.garage.data.local.entity.toDomain
import com.example.garage.data.local.entity.toEntity
import com.example.garage.data.remote.FirestoreServiceRecordSource
import com.example.garage.data.remote.ReceiptExtractor
import com.example.garage.data.remote.StorageService
import com.example.garage.domain.model.ReceiptExtraction
import com.example.garage.domain.model.ServiceRecord
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
class ServiceRecordRepository @Inject constructor(
    private val dao: ServiceRecordDao,
    private val remote: FirestoreServiceRecordSource,
    private val authRepository: AuthRepository,
    private val receiptExtractor: ReceiptExtractor,
    private val storageService: StorageService
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun tryPush() {
        authRepository.currentUserId?.let { uid ->
            repositoryScope.launch {
                pushUnsynced(uid)
            }
        }
    }

    fun observeAll(includeArchived: Boolean = false): Flow<List<ServiceRecord>> =
        if (includeArchived) {
            dao.observeArchived().map { list -> list.map { it.toDomain() } }
        } else {
            dao.observeAll().map { list -> list.map { it.toDomain() } }
        }

    fun observeForVehicle(vehicleId: String): Flow<List<ServiceRecord>> =
        dao.observeForVehicle(vehicleId).map { list -> list.map { it.toDomain() } }

    suspend fun getRecordsForVehicle(vehicleId: String): List<ServiceRecord> =
        dao.getForVehicle(vehicleId).map { it.toDomain() }

    fun observeById(id: String): Flow<ServiceRecord?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun getRecordById(id: String): ServiceRecord? =
        dao.getById(id)?.toDomain()

    suspend fun addRecord(record: ServiceRecord): ServiceRecord {
        val withId = if (record.id.isBlank()) record.copy(id = UUID.randomUUID().toString()) else record
        dao.upsert(withId.toEntity(isSynced = false))
        tryPush()
        return withId
    }

    suspend fun deleteRecord(id: String) {
        dao.markDeleted(id)
        tryPush()
    }

    suspend fun deleteRecordsForVehicle(vehicleId: String) {
        dao.markDeletedForVehicle(vehicleId)
        tryPush()
    }

    suspend fun extractReceiptData(bitmap: Bitmap): ReceiptExtraction =
        receiptExtractor.extract(bitmap)

    suspend fun uploadReceipt(bitmap: Bitmap): String? {
        val uid = authRepository.currentUserId ?: return null
        return storageService.uploadReceipt(uid, bitmap)
    }

    fun startRemoteListener(scope: CoroutineScope, uid: String) {
        scope.launch {
            remote.observe(uid)
                .catch { }
                .collect { remoteRecords -> dao.sync(remoteRecords) }
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
