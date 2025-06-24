package ru.openfs.lbapi.model

import java.time.LocalDate

data class PromiseSettings(
    val isAllowed: Boolean,
    val dateLimit: LocalDate,
    val minAmount: Double,
    val maxAmount: Double,
    val limitAmount: Double
)