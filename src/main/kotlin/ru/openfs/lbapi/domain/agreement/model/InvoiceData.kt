package ru.openfs.lbapi.domain.agreement.model

data class InvoiceData(
    val vgId: Long,
    val tarId: Long,
    val serviceName: String,
    val charges: Double,
)