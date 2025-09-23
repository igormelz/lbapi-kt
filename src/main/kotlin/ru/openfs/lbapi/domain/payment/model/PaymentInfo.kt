package ru.openfs.lbapi.domain.payment.model

data class PaymentInfo(
    val paymentType: String,
    val paymentCode: String,
    val paymentId: String,
    val paymentAmount: Double,
    val paymentDate: String,
)