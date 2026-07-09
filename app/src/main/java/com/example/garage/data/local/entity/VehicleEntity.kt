package com.example.garage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.garage.domain.model.Vehicle

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    val year: String,
    val make: String,
    val model: String,
    val submodel: String?,
    val engine: String?,
    val notes: String?,
    val odometer: Int,
    val photoUrl: String?,
    val createdAt: Long,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

fun VehicleEntity.toDomain() = Vehicle(
    id = id,
    year = year,
    make = make,
    model = model,
    submodel = submodel,
    engine = engine,
    notes = notes,
    odometer = odometer,
    photoUrl = photoUrl,
    createdAt = createdAt
)

fun Vehicle.toEntity(isSynced: Boolean = false) = VehicleEntity(
    id = id,
    year = year,
    make = make,
    model = model,
    submodel = submodel,
    engine = engine,
    notes = notes,
    odometer = odometer,
    photoUrl = photoUrl,
    createdAt = createdAt,
    isSynced = isSynced
)
