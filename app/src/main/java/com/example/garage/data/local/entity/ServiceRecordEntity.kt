package com.example.garage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.garage.domain.model.ServiceCategory
import com.example.garage.domain.model.ServiceRecord

@Entity(tableName = "service_records")
data class ServiceRecordEntity(
    @PrimaryKey val id: String = "",
    val vehicleId: String = "",
    val title: String = "",
    val category: String = "",
    val description: String? = null,
    val odometer: Int = 0,
    val cost: Double? = null,
    val date: Long = 0L,
    val receiptPhotoUrl: String? = null,
    val createdAt: Long = 0L,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

fun ServiceRecordEntity.toDomain() = ServiceRecord(
    id = id,
    vehicleId = vehicleId,
    title = title,
    category = runCatching { ServiceCategory.valueOf(category) }.getOrDefault(ServiceCategory.OTHER),
    description = description,
    odometer = odometer,
    cost = cost,
    date = date,
    receiptPhotoUrl = receiptPhotoUrl,
    createdAt = createdAt
)

fun ServiceRecord.toEntity(isSynced: Boolean = false) = ServiceRecordEntity(
    id = id,
    vehicleId = vehicleId,
    title = title,
    category = category.name,
    description = description,
    odometer = odometer,
    cost = cost,
    date = date,
    receiptPhotoUrl = receiptPhotoUrl,
    createdAt = createdAt,
    isSynced = isSynced
)
