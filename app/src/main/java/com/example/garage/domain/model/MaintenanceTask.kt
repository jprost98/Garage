package com.example.garage.domain.model

/**
 * A maintenance task the user wants reminders for. Recurring tasks repeat
 * on a mileage or time interval; single tasks have one fixed due point.
 */
data class MaintenanceTask(
    val id: String,
    val vehicleId: String,
    val name: String,
    val type: TaskType,
    val category: ServiceCategory = ServiceCategory.OTHER,
    val notes: String? = null,
    val intervalMiles: Int? = null,
    val intervalMonths: Int? = null,
    val startingOdometer: Int? = null,
    val lastDoneDate: Long? = null,
    val lastDoneOdometer: Int? = null,
    val dueDate: Long? = null,
    val dueOdometer: Int? = null,
    val completed: Boolean = false,
    val associatedRecordId: String? = null,
    val lastNotifiedUrgencyLevel: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TaskType { RECURRING, SINGLE }

/**
 * How urgent a task is right now, derived at read time from the vehicle's
 * current odometer and today's date rather than stored â€” so it's always
 * fresh and never drifts out of sync.
 */
sealed class TaskUrgency {
    data class Overdue(val milesPast: Int? = null, val daysPast: Int? = null) : TaskUrgency()
    data class DueSoon(val milesLeft: Int? = null, val daysLeft: Int? = null) : TaskUrgency()
    data class Upcoming(val milesLeft: Int? = null, val daysLeft: Int? = null) : TaskUrgency()
    object UpToDate : TaskUrgency()
}
