package com.example.garage.data.remote

import com.example.garage.data.local.entity.VehicleEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Talks to Firestore for one collection. Repositories are the only
 * callers - this class knows nothing about Room.
 */
class FirestoreVehicleSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection("vehicles")

    fun observe(uid: String): Flow<List<VehicleEntity>> = callbackFlow {
        val registration: ListenerRegistration = collection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val vehicles = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(VehicleEntity::class.java)?.copy(id = doc.id, isSynced = true)
            } ?: emptyList()
            trySend(vehicles)
        }
        awaitClose { registration.remove() }
    }

    suspend fun push(uid: String, vehicle: VehicleEntity) {
        collection(uid).document(vehicle.id).set(vehicle).await()
    }

    suspend fun delete(uid: String, vehicleId: String) {
        collection(uid).document(vehicleId).delete().await()
    }
}
