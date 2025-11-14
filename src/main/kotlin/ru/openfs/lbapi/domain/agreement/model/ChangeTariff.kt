package ru.openfs.lbapi.domain.agreement.model

data class ChangeTariff(
    val tarId: Long,
    val tarName: String,
    val rent: Double,
    val changeDateTime: String,
)