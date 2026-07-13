package com.example.garage.data.remote

import android.graphics.Bitmap
import com.example.garage.domain.model.ReceiptExtraction
import com.example.garage.domain.model.ServiceCategory
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class ReceiptExtractor @Inject constructor(
    private val model: GenerativeModel
) {
    suspend fun extract(bitmap: Bitmap): ReceiptExtraction = withContext(Dispatchers.IO) {
        val prompt = content {
            image(bitmap)
            text("""
                Extract data from this automotive service receipt. 
                Return a JSON object with the following fields:
                - title: a short descriptive title of the service (e.g., "Full Synthetic Oil Change")
                - category: one of OIL_CHANGE, TIRES, BRAKES, BATTERY, FLUIDS, INSPECTION, or OTHER
                - date: the service date as a Unix timestamp in milliseconds
                - odometer: the mileage reading as an integer
                - cost: the total cost as a double
                - description: a brief summary of what was done
                
                Only return the raw JSON object, no markdown formatting.
            """.trimIndent())
        }

        val response = model.generateContent(listOf(prompt))
        val text = response.text ?: return@withContext ReceiptExtraction()
        
        try {
            val json = JSONObject(text.trim().removeSurrounding("```json", "```"))
            ReceiptExtraction(
                title = json.optString("title").takeIf { it.isNotBlank() },
                category = json.optString("category").let { cat ->
                    runCatching { ServiceCategory.valueOf(cat) }.getOrNull()
                },
                date = json.optLong("date").takeIf { it > 0 },
                odometer = json.optInt("odometer").takeIf { it > 0 },
                cost = json.optDouble("cost").takeIf { !it.isNaN() },
                description = json.optString("description").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            ReceiptExtraction()
        }
    }
}
