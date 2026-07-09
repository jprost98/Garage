package com.example.garage.data.local.dao

import androidx.room.*
import com.example.garage.data.local.entity.ServiceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRecordDao {
    @Query("SELECT * FROM service_records WHERE isDeleted = 0 ORDER BY date DESC")
    fun observeAll(): Flow<List<ServiceRecordEntity>>

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY date DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<ServiceRecordEntity>>

    @Query("SELECT * FROM service_records WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ServiceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ServiceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<ServiceRecordEntity>)

    @Query("UPDATE service_records SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE service_records SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)
}
