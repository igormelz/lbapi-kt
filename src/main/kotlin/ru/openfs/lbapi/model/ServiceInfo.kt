package ru.openfs.lbapi.model

data class ServiceInfo(
    val id: Long,
    val login: String,
    val address: String,
    val tarType: Long,
    val tarName: String,
    val tarShape: String,
    val tarRent: Double,
    val blocked: Boolean,
    val blockedType: Long,
    val rentPeriod: String,
)
