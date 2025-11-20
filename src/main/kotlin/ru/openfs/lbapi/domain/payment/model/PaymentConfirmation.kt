package ru.openfs.lbapi.domain.payment.model

data class PaymentConfirmation(
    val confirmationUrl: String,
    val paymentId: String,
)