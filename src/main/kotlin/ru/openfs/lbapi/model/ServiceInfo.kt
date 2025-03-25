package ru.openfs.lbapi.model

data class ServiceInfo(
    val id: Long,
    val login: String,
    val address: String,
    val tarName: String,
    val tarShape: String,
    val tarType: Long,
    val tarRent: Double,
    val blocked: Boolean,
)
