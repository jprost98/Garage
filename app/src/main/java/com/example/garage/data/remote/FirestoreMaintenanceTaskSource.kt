package com.example.garage.data.remote

import com.example.garage.data.local.entity.MaintenanceTaskEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreMaintenanceTaskSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection("tasks")

    fun observe(uid: String): Flow<List<MaintenanceTaskEntity>> = callbackFlow {
        val registration: ListenerRegistration = collection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                // If UNAVAILABLE (offline), we don't close the flow. 
                // Firestore will automatically retry when connectivity returns.
                if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    return@addSnapshotListener
                }
                close(error)
                return@addSnapshotListener
            }
            val tasks = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(MaintenanceTaskEntity::class.java)?.copy(id = doc.id, isSynced = true)
            } ?: emptyList()
            trySend(tasks)
        }
        awaitClose { registration.remove() }
    }

    suspend fun push(uid: String, task: MaintenanceTaskEntity) {
        collection(uid).document(task.id).set(task).await()
    }

    suspend fun delete(uid: String, taskId: String) {
        collection(uid).document(taskId).delete().await()
    }
}
