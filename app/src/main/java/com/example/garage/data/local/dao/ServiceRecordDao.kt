package com.example.garage.data.local.dao

import androidx.room.*
import com.example.garage.data.local.entity.ServiceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRecordDao {
    @Query("""
        SELECT r.* FROM service_records r 
        JOIN vehicles v ON r.vehicleId = v.id 
        WHERE r.isDeleted = 0 AND v.isArchived = 0 AND v.isDeleted = 0
        ORDER BY r.date DESC
    """)
    fun observeAll(): Flow<List<ServiceRecordEntity>>

    @Query("""
        SELECT r.* FROM service_records r 
        JOIN vehicles v ON r.vehicleId = v.id 
        WHERE r.isDeleted = 0 AND v.isArchived = 1 AND v.isDeleted = 0
        ORDER BY r.date DESC
    """)
    fun observeArchived(): Flow<List<ServiceRecordEntity>>

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY date DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<ServiceRecordEntity>>

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId AND isDeleted = 0")
    suspend fun getForVehicle(vehicleId: String): List<ServiceRecordEntity>

    @Query("SELECT * FROM service_records WHERE id = :id")
    fun observeById(id: String): Flow<ServiceRecordEntity?>

    @Query("SELECT * FROM service_records WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ServiceRecordEntity>

    @Query("SELECT * FROM service_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ServiceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ServiceRecordEntity)

    @Transaction
    suspend fun sync(remoteRecords: List<ServiceRecordEntity>) {
        val remoteIds = remoteRecords.map { it.id }.toSet()
        deleteSyncedNotIn(remoteIds)
        remoteRecords.forEach { remote ->
            val local = getById(remote.id)
            if (local == null || local.isSynced) {
                upsert(remote.copy(isSynced = true, isDeleted = false))
            }
        }
    }

    @Query("DELETE FROM service_records WHERE isSynced = 1 AND id NOT IN (:remoteIds)")
    suspend fun deleteSyncedNotIn(remoteIds: Set<String>)

    @Query("UPDATE service_records SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE service_records SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE service_records SET isDeleted = 1, isSynced = 0 WHERE vehicleId = :vehicleId")
    suspend fun markDeletedForVehicle(vehicleId: String)

    @Query("DELETE FROM service_records WHERE id = :id")
    suspend fun hardDelete(id: String)
}
