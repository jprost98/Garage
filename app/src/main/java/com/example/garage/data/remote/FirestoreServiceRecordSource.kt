package com.example.garage.data.remote

import com.example.garage.data.local.entity.ServiceRecordEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreServiceRecordSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection("records")

    fun observe(uid: String): Flow<List<ServiceRecordEntity>> = callbackFlow {
        val registration: ListenerRegistration = collection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val records = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ServiceRecordEntity::class.java)?.copy(id = doc.id, isSynced = true)
            } ?: emptyList()
            trySend(records)
        }
        awaitClose { registration.remove() }
    }

    suspend fun push(uid: String, record: ServiceRecordEntity) {
        collection(uid).document(record.id).set(record).await()
    }

    suspend fun delete(uid: String, recordId: String) {
        collection(uid).document(recordId).delete().await()
    }
}
