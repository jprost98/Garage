package com.example.garage.data.local.dao

import androidx.room.*
import com.example.garage.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE isDeleted = 0 AND isArchived = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE isDeleted = 0 AND isArchived = 1 ORDER BY createdAt DESC")
    fun observeArchived(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE isSynced = 0")
    suspend fun getUnsynced(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vehicle: VehicleEntity)

    @Transaction
    suspend fun sync(remoteVehicles: List<VehicleEntity>) {
        val remoteIds = remoteVehicles.map { it.id }.toSet()
        
        // 1. Delete local records that are synced but missing from remote (deleted elsewhere)
        deleteSyncedNotIn(remoteIds)

        // 2. For each remote record, upsert it only if local version is NOT dirty
        remoteVehicles.forEach { remote ->
            val local = getById(remote.id)
            if (local == null || local.isSynced) {
                upsert(remote.copy(isSynced = true, isDeleted = false))
            }
        }
    }

    @Query("DELETE FROM vehicles WHERE isSynced = 1 AND id NOT IN (:remoteIds)")
    suspend fun deleteSyncedNotIn(remoteIds: Set<String>)

    @Query("UPDATE vehicles SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE vehicles SET odometer = :odometer, isSynced = 0 WHERE id = :id")
    suspend fun updateOdometer(id: String, odometer: Int)

    @Query("UPDATE vehicles SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE vehicles SET isArchived = :archived, isSynced = 0 WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun hardDelete(id: String)
}
