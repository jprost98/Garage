package com.example.garage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.TaskType
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "maintenance_tasks")
@IgnoreExtraProperties
data class MaintenanceTaskEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val name: String,
    val type: String,
    val notes: String?,
    val intervalMiles: Int?,
    val intervalMonths: Int?,
    val lastDoneDate: Long?,
    val lastDoneOdometer: Int?,
    val dueDate: Long?,
    val dueOdometer: Int?,
    val completed: Boolean,
    val createdAt: Long,
    @get:Exclude @set:Exclude var isSynced: Boolean = false,
    @get:PropertyName("deleted") @set:PropertyName("deleted") var isDeleted: Boolean = false
)

fun MaintenanceTaskEntity.toDomain() = MaintenanceTask(
    id = id,
    vehicleId = vehicleId,
    name = name,
    type = runCatching { TaskType.valueOf(type) }.getOrDefault(TaskType.SINGLE),
    notes = notes,
    intervalMiles = intervalMiles,
    intervalMonths = intervalMonths,
    lastDoneDate = lastDoneDate,
    lastDoneOdometer = lastDoneOdometer,
    dueDate = dueDate,
    dueOdometer = dueOdometer,
    completed = completed,
    createdAt = createdAt
)

fun MaintenanceTask.toEntity(isSynced: Boolean = false) = MaintenanceTaskEntity(
    id = id,
    vehicleId = vehicleId,
    name = name,
    type = type.name,
    notes = notes,
    intervalMiles = intervalMiles,
    intervalMonths = intervalMonths,
    lastDoneDate = lastDoneDate,
    lastDoneOdometer = lastDoneOdometer,
    dueDate = dueDate,
    dueOdometer = dueOdometer,
    completed = completed,
    createdAt = createdAt,
    isSynced = isSynced
)
