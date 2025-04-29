package ru.openfs.lbapi.utils

import java.time.LocalDate

object FormatUtil {
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

}