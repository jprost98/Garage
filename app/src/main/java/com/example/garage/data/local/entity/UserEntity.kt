package com.example.garage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.garage.domain.model.GarageUser
import com.google.firebase.firestore.IgnoreExtraProperties

@Entity(tableName = "users")
@IgnoreExtraProperties
data class UserEntity(
    @PrimaryKey val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val birthday: Long? = null,
    var isSynced: Boolean = false
)

fun UserEntity.toDomain() = GarageUser(
    uid = uid,
    email = email,
    displayName = displayName,
    firstName = firstName,
    lastName = lastName,
    birthday = birthday
)

fun GarageUser.toEntity(isSynced: Boolean = false) = UserEntity(
    uid = uid,
    email = email,
    displayName = displayName,
    firstName = firstName,
    lastName = lastName,
    birthday = birthday,
    isSynced = isSynced
)
