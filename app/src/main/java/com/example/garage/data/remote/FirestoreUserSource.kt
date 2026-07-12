package com.example.garage.data.remote

import com.example.garage.data.local.entity.UserEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreUserSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun userDocument(uid: String) = firestore.collection("users").document(uid)

    suspend fun push(user: UserEntity) {
        userDocument(user.uid).set(user).await()
    }

    suspend fun get(uid: String): UserEntity? {
        return userDocument(uid).get().await().toObject(UserEntity::class.java)
    }
}
