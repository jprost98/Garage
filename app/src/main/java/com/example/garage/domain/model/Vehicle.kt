package com.example.garage.domain.model

/**
 * A vehicle owned by the signed-in user. This is the domain-layer model:
 * it has no knowledge of Room or Firestore, so UI and use-cases never
 * depend on persistence details.
 */
data class Vehicle(
    val id: String,
    val year: String,
    val make: String,
    val model: String,
    val submodel: String? = null,
    val engine: String? = null,
    val notes: String? = null,
    val odometer: Int = 0,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val title: String
        get() = listOfNotNull(year, make, model, submodel).joinToString(" ")
}
