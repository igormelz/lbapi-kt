package ru.openfs.lbapi.model

data class UserBlockTemplate(
    val durationMin: Long,
    val durationMax: Long,
    val numAvailableBlocks: Long,
    val positiveBalance: Boolean,
)

data class UserBlockSchedule(
    val id: Long,
    val createDate: String,
    val validFrom: String,
    val validUntil: String,
    val isActive: Boolean,
)