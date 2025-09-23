package ru.openfs.lbapi.domain.blocking.model

data class UserBlockSchedule(
    val id: Long,
    val createDate: String,
    val validFrom: String,
    val validUntil: String,
    val isActive: Boolean,
)