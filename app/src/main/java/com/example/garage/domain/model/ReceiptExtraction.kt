package com.example.garage.domain.model

data class ReceiptExtraction(
    val title: String? = null,
    val category: ServiceCategory? = null,
    val date: Long? = null,
    val odometer: Int? = null,
    val cost: Double? = null,
    val description: String? = null
)
