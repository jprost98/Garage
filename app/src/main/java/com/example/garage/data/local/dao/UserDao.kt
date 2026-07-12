package com.example.garage.data.local.dao

import androidx.room.*
import com.example.garage.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun observeById(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE isSynced = 0")
    suspend fun getUnsynced(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("UPDATE users SET isSynced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)
}
