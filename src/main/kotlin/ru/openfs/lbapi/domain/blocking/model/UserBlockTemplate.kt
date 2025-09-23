package ru.openfs.lbapi.domain.blocking.model

data class UserBlockTemplate(
    val durationMin: Long,
    val durationMax: Long,
    val numAvailableBlocks: Long,
    val positiveBalance: Boolean,
)