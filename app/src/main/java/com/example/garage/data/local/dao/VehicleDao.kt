package com.example.garage.data.local.dao

import androidx.room.*
import com.example.garage.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE isSynced = 0")
    suspend fun getUnsynced(): List<VehicleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vehicle: VehicleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(vehicles: List<VehicleEntity>)

    @Query("UPDATE vehicles SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE vehicles SET odometer = :odometer, isSynced = 0 WHERE id = :id")
    suspend fun updateOdometer(id: String, odometer: Int)

    @Query("UPDATE vehicles SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)
}
