package com.example.garage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.garage.domain.model.Vehicle
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "vehicles")
@IgnoreExtraProperties
data class VehicleEntity(
    @PrimaryKey val id: String = "",
    val year: String = "",
    val make: String = "",
    val model: String = "",
    val submodel: String? = null,
    val engine: String? = null,
    val notes: String? = null,
    val odometer: Int = 0,
    val photoUrl: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = 0L,
    @get:Exclude @set:Exclude var isSynced: Boolean = false,
    @get:PropertyName("deleted") @set:PropertyName("deleted") var isDeleted: Boolean = false
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
    isArchived = isArchived,
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
    isArchived = isArchived,
    createdAt = createdAt,
    isSynced = isSynced
)
