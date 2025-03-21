package ru.openfs.lbapi.model

data class RecPayment(
    val id: Long,
    val number: String,
    val payment: Double,
    val balance: Double,
    val isCredit: Boolean,
)
