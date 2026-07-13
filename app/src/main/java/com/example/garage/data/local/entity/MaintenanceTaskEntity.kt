package com.example.garage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.ServiceCategory
import com.example.garage.domain.model.TaskType
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "maintenance_tasks")
@IgnoreExtraProperties
data class MaintenanceTaskEntity(
    @PrimaryKey val id: String = "",
    val vehicleId: String = "",
    val name: String = "",
    val type: String = "",
    val category: String = "OTHER",
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
    val createdAt: Long = 0L,
    @get:Exclude @set:Exclude var isSynced: Boolean = false,
    @get:PropertyName("deleted") @set:PropertyName("deleted") var isDeleted: Boolean = false
)

fun MaintenanceTaskEntity.toDomain() = MaintenanceTask(
    id = id,
    vehicleId = vehicleId,
    name = name,
    type = runCatching { TaskType.valueOf(type) }.getOrDefault(TaskType.SINGLE),
    category = runCatching { ServiceCategory.valueOf(category) }.getOrDefault(ServiceCategory.OTHER),
    notes = notes,
    intervalMiles = intervalMiles,
    intervalMonths = intervalMonths,
    startingOdometer = startingOdometer,
    lastDoneDate = lastDoneDate,
    lastDoneOdometer = lastDoneOdometer,
    dueDate = dueDate,
    dueOdometer = dueOdometer,
    completed = completed,
    associatedRecordId = associatedRecordId,
    lastNotifiedUrgencyLevel = lastNotifiedUrgencyLevel,
    createdAt = createdAt
)

fun MaintenanceTask.toEntity(isSynced: Boolean = false) = MaintenanceTaskEntity(
    id = id,
    vehicleId = vehicleId,
    name = name,
    type = type.name,
    category = category.name,
    notes = notes,
    intervalMiles = intervalMiles,
    intervalMonths = intervalMonths,
    startingOdometer = startingOdometer,
    lastDoneDate = lastDoneDate,
    lastDoneOdometer = lastDoneOdometer,
    dueDate = dueDate,
    dueOdometer = dueOdometer,
    completed = completed,
    associatedRecordId = associatedRecordId,
    lastNotifiedUrgencyLevel = lastNotifiedUrgencyLevel,
    createdAt = createdAt,
    isSynced = isSynced
)
