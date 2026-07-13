package com.example.garage.data.remote

import android.graphics.Bitmap
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

class StorageService @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadReceipt(uid: String, bitmap: Bitmap): String {
        val storageRef = storage.reference.child("users/$uid/receipts/${UUID.randomUUID()}.jpg")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()
        
        storageRef.putBytes(data).await()
        return storageRef.downloadUrl.await().toString()
    }
}
