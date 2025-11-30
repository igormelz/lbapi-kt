package ru.openfs.lbapi.domain.tarif.model

data class AvailableTariff(
    val tarId: Long,
    val tarDescr: String,
    val tarRent: Double,
    val rateLevel: Int, // 1, 6, 12
    val shape: Int,
    val serviceCat: Long? = null,
)
