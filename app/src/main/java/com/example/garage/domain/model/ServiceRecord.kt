package com.example.garage.domain.model

/**
 * A single logged maintenance event (oil change, brake job, etc.) tied to
 * one vehicle.
 */
data class ServiceRecord(
    val id: String,
    val vehicleId: String,
    val title: String,
    val category: ServiceCategory,
    val description: String? = null,
    val odometer: Int,
    val cost: Double? = null,
    val date: Long,
    val receiptPhotoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ServiceCategory(val label: String) {
    OIL_CHANGE("Oil change"),
    TIRES("Tires"),
    BRAKES("Brakes"),
    BATTERY("Battery"),
    FLUIDS("Fluids"),
    INSPECTION("Inspection"),
    OTHER("Other")
}
