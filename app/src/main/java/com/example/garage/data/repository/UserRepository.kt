package com.example.garage.data.repository

import com.example.garage.data.local.dao.UserDao
import com.example.garage.data.local.entity.toDomain
import com.example.garage.data.local.entity.toEntity
import com.example.garage.data.remote.FirestoreUserSource
import com.example.garage.domain.model.GarageUser
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val firestoreUserSource: FirestoreUserSource
) {
    fun observeUser(uid: String) = userDao.observeById(uid).map { it?.toDomain() }

    suspend fun saveUserLocally(user: GarageUser) {
        userDao.upsert(user.toEntity(isSynced = false))
    }

    suspend fun pushUnsynced() {
        val unsynced = userDao.getUnsynced()
        unsynced.forEach { userEntity ->
            try {
                firestoreUserSource.push(userEntity)
                userDao.markSynced(userEntity.uid)
            } catch (e: Exception) {
                // Log or handle sync error
            }
        }
    }

    suspend fun fetchRemoteUser(uid: String) {
        try {
            val remoteUser = firestoreUserSource.get(uid)
            if (remoteUser != null) {
                userDao.upsert(remoteUser.copy(isSynced = true))
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}
