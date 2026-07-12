package com.example.garage.domain.model

data class GarageUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val firstName: String? = null,
    val lastName: String? = null,
    val birthday: Long? = null
)
