package com.example.garage.data.local.dao

import androidx.room.*
import com.example.garage.data.local.entity.MaintenanceTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceTaskDao {
    @Query("SELECT * FROM maintenance_tasks WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MaintenanceTaskEntity>>

    @Query("SELECT * FROM maintenance_tasks WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<MaintenanceTaskEntity>>

    @Query("SELECT * FROM maintenance_tasks WHERE isSynced = 0")
    suspend fun getUnsynced(): List<MaintenanceTaskEntity>

    @Query("SELECT * FROM maintenance_tasks WHERE isDeleted = 0 AND completed = 0")
    suspend fun getActiveTasks(): List<MaintenanceTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: MaintenanceTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<MaintenanceTaskEntity>)

    @Query("UPDATE maintenance_tasks SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE maintenance_tasks SET completed = :completed, isSynced = 0 WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean)

    @Query("UPDATE maintenance_tasks SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)
}
