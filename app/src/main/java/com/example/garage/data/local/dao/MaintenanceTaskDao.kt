package com.example.garage.data.local.dao

import androidx.room.*
import com.example.garage.data.local.entity.MaintenanceTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceTaskDao {
    @Query("""
        SELECT t.* FROM maintenance_tasks t
        JOIN vehicles v ON t.vehicleId = v.id
        WHERE t.isDeleted = 0 AND v.isArchived = 0 AND v.isDeleted = 0
        ORDER BY t.createdAt DESC
    """)
    fun observeAll(): Flow<List<MaintenanceTaskEntity>>

    @Query("SELECT * FROM maintenance_tasks WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<MaintenanceTaskEntity>>

    @Query("SELECT * FROM maintenance_tasks WHERE isSynced = 0")
    suspend fun getUnsynced(): List<MaintenanceTaskEntity>

    @Query("SELECT * FROM maintenance_tasks WHERE isDeleted = 0 AND completed = 0")
    suspend fun getActiveTasks(): List<MaintenanceTaskEntity>

    @Query("SELECT * FROM maintenance_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MaintenanceTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: MaintenanceTaskEntity)

    @Transaction
    suspend fun sync(remoteTasks: List<MaintenanceTaskEntity>) {
        val remoteIds = remoteTasks.map { it.id }.toSet()
        deleteSyncedNotIn(remoteIds)
        remoteTasks.forEach { remote ->
            val local = getById(remote.id)
            if (local == null || local.isSynced) {
                upsert(remote.copy(isSynced = true, isDeleted = false))
            }
        }
    }

    @Query("DELETE FROM maintenance_tasks WHERE isSynced = 1 AND id NOT IN (:remoteIds)")
    suspend fun deleteSyncedNotIn(remoteIds: Set<String>)

    @Query("UPDATE maintenance_tasks SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE maintenance_tasks SET completed = :completed, isSynced = 0 WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean)

    @Query("UPDATE maintenance_tasks SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE maintenance_tasks SET isDeleted = 1, isSynced = 0 WHERE vehicleId = :vehicleId")
    suspend fun markDeletedForVehicle(vehicleId: String)

    @Query("DELETE FROM maintenance_tasks WHERE id = :id")
    suspend fun hardDelete(id: String)
}
