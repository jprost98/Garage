package com.example.garage.data.repository

import com.example.garage.domain.model.GarageUser
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) {
    val currentUserId: String? get() = firebaseAuth.currentUser?.uid

    val currentUser: GarageUser?
        get() = firebaseAuth.currentUser?.let {
            GarageUser(uid = it.uid, email = it.email, displayName = it.displayName)
        }

    fun observeAuthState(): Flow<GarageUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser?.let {
                GarageUser(uid = it.uid, email = it.email, displayName = it.displayName)
            }
            trySend(user)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        result.user?.let {
            userRepository.saveUserLocally(GarageUser(it.uid, it.email, it.displayName))
        }
        Unit
    }

    suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        result.user?.let {
            userRepository.saveUserLocally(GarageUser(it.uid, it.email, it.displayName))
        }
        Unit
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> = runCatching {
        firebaseAuth.currentUser?.updateEmail(newEmail)?.await()
        Unit
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Unit
    }

    fun signOut() = firebaseAuth.signOut()
}
