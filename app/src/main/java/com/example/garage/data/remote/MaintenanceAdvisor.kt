package com.example.garage.data.remote

import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.domain.model.ServiceCategory
import com.example.garage.domain.model.TaskType
import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class SuggestionResult(
    val tasks: List<MaintenanceTask>,
    val explanation: String
)

class MaintenanceAdvisor @Inject constructor(
    private val model: GenerativeModel,
    private val json: Json
) {
    suspend fun suggestTasks(
        year: String,
        make: String,
        modelName: String,
        existingTasks: List<MaintenanceTask> = emptyList(),
        recentRecords: List<ServiceRecord> = emptyList()
    ): SuggestionResult = withContext(Dispatchers.IO) {
        val existingTaskNames = existingTasks.joinToString { it.name }
        val recentServiceSummary = recentRecords.asSequence().take(10).joinToString { 
            "${it.title} (${it.category.name}) at ${it.odometer} mi - Cost: $${it.cost ?: 0.0}" 
        }

        val template = """
            {{role "system"}}
            You are the Garage AI Assistant. Your goal is to help users maintain their vehicles. You have access to their vehicle data and service history. Keep answers concise, helpful, and focused on preventative maintenance. 
            
            Return a JSON structure:
            {
                "explanation": "string",
                "tasks": [
                    {
                        "name": "string",
                        "type": "RECURRING",
                        "category": "OTHER",
                        "intervalMiles": 0,
                        "intervalMonths": 0,
                        "notes": "string"
                    }
                ]
            }
            Do not include markdown code blocks. Always return a valid JSON object.

            {{role "user"}}
            Analyze the following vehicle information. Please provide a summary of the vehicle, some data analytics over its service history, and your recommendations on maintenance schedules (that are not already present) in the explanation field. Then, generate a list of recommended maintenance tasks for the vehicle.
            Vehicle: $year $make $modelName
            Current Odometer: ${recentRecords.maxOfOrNull { it.odometer } ?: 0}
            Existing tasks for this vehicle: $existingTaskNames
            Recent service history: $recentServiceSummary
        """.trimIndent()

        val response = try {
            model.generateContent(template)
        } catch (e: Exception) {
            return@withContext SuggestionResult(emptyList(), "Could not retrieve AI suggestions: ${e.message}")
        }
        
        val responseText = response.text ?: return@withContext SuggestionResult(emptyList(), "Could not retrieve AI suggestions.")
        
        try {
            val cleanText = responseText.trim().replace("```json", "").replace("```", "").trim()
            val result = json.decodeFromString<AiMaintenanceResponse>(cleanText)
            
            val suggestions = result.tasks.map { task ->
                MaintenanceTask(
                    id = "", 
                    vehicleId = "", 
                    name = task.name,
                    type = runCatching { TaskType.valueOf(task.type) }.getOrDefault(TaskType.RECURRING),
                    category = runCatching { ServiceCategory.valueOf(task.category) }.getOrDefault(ServiceCategory.OTHER),
                    startingOdometer = 0,
                    intervalMiles = task.intervalMiles?.takeIf { it > 0 },
                    intervalMonths = task.intervalMonths?.takeIf { it > 0 },
                    notes = task.notes?.takeIf { it.isNotBlank() }
                )
            }
            SuggestionResult(suggestions, result.explanation)
        } catch (e: Exception) {
            SuggestionResult(emptyList(), "Could not parse AI suggestions: ${e.message}")
        }
    }
}

@kotlinx.serialization.Serializable
private data class AiMaintenanceResponse(
    val explanation: String,
    val tasks: List<AiMaintenanceSuggestion>
)

@kotlinx.serialization.Serializable
private data class AiMaintenanceSuggestion(
    val name: String,
    val type: String,
    val category: String,
    val intervalMiles: Int? = null,
    val intervalMonths: Int? = null,
    val notes: String? = null
)
