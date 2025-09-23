package ru.openfs.lbapi.common.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FormatUtil {
    private val DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun convertBpsToReadableFormat(bps: Long): String {
        if (bps <= 0) return ""

        val units = arrayOf("Kbps", "Mbps", "Gbps", "Tbps")
        var value = bps//.toDouble()
        var unitIndex = 0

        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }

        return "$value ${units[unitIndex]}"
    }

    fun getDateStartMonth() = LocalDate.now().withDayOfMonth(1).toString()
    fun getTomorrowDate() = LocalDate.now().plusDays(1L).toString()
    fun getDateStartNextMonth() = LocalDate.now().plusMonths(1L).withDayOfMonth(1).toString()
    fun String.isDateTimeAfterNow() = LocalDateTime.parse(this, DATE_TIME_PATTERN).isAfter(LocalDateTime.now())

}