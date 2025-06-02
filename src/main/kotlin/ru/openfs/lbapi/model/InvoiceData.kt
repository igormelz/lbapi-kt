package ru.openfs.lbapi.model

data class InvoiceData(
    val vgId: Long,
    val tarId: Long,
    val serviceName: String,
    val charges: Double,
)
