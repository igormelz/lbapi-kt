package ru.openfs.lbapi.domain.agreement.model

import java.time.LocalDate
import java.time.LocalDateTime

data class ChangeTariff(
    val tarId: Long,
    val tarName: String,
    val rent: Double,
    val changeDateTime: LocalDate,
)