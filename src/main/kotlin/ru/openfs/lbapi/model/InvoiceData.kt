package ru.openfs.lbapi.model

data class InvoiceData(
    val period: String,
    val charges: Double,
    val currentPayments: Double,
)
