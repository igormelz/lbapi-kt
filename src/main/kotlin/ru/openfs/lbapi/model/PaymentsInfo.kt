package ru.openfs.lbapi.model

data class PaymentsInfo(
    val paymentType: String,
    val paymentCode: String,
    val paymentId: String,
    val paymentAmount: Double,
    val paymentDate: String,
)
