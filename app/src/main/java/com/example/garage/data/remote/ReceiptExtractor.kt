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
                You are a highly precise data extraction system. Your sole task is to extract authentic, real-world data from the provided image of an automotive service receipt.
                
                CRITICAL INSTRUCTION: First, analyze the image to determine if it is actually a legible automotive service receipt, invoice, or work order.
                If the image is blank, solid color, blurry, unreadable, irrelevant (such as a selfie or random object), or not an automotive service document:
                - You must NOT guess, assume, or hallucinate any details.
                - Do NOT invent a fake service, price, date, or title.
                - Return a completely empty JSON object: {}
                
                If the image is a valid, legible automotive service receipt:
                - Extract ONLY the details that are physically and clearly printed on the document.
                - Do NOT invent or estimate values that are missing.
                - Return a JSON object with the following fields (set the field to null if it is missing or not visible on the receipt):
                  * title: a short descriptive title of the service (e.g., "Full Synthetic Oil Change")
                  * category: one of OIL_CHANGE, TIRES, BRAKES, BATTERY, FLUIDS, INSPECTION, or OTHER
                  * date: the service date as a Unix timestamp in milliseconds
                  * odometer: the mileage reading as an integer
                  * cost: the total cost as a double
                  * description: a brief summary of what was done
                
                Only return the raw JSON object, no markdown formatting, and no markdown blocks (do NOT wrap the JSON in ```json or ```).
            """.trimIndent())
        }

        val response = model.generateContent(listOf(prompt))
        val text = response.text ?: return@withContext ReceiptExtraction()
        
        try {
            val jsonRegex = """(?s)\{.*\}""".toRegex()
            val jsonMatch = jsonRegex.find(text)
            val jsonString = jsonMatch?.value ?: text

            val json = JSONObject(jsonString.trim())
            ReceiptExtraction(
                title = json.optString("title").takeIf { it.isNotBlank() },
                category = json.optString("category").let { cat ->
                    runCatching { ServiceCategory.valueOf(cat.uppercase().trim()) }.getOrNull()
                },
                date = json.optLong("date", -1L).takeIf { it > 0 },
                odometer = json.optInt("odometer", -1).takeIf { it >= 0 },
                cost = json.optDouble("cost", -1.0).takeIf { it >= 0.0 },
                description = json.optString("description").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            ReceiptExtraction()
        }
    }
}
