package ru.openfs.lbapi.common.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

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
    fun getDateTimeString(): String? = DATE_TIME_PATTERN.format(LocalDateTime.now())

    fun nextPaymentDate(
        startDate: LocalDate,
        intervalMonths: Long = 6,
        today: LocalDate = LocalDate.now()
    ): LocalDate {
        if (today.isBefore(startDate)) return startDate

        // total months difference (may be 0)
        val totalMonths = (today.year - startDate.year) * 12L +
                (today.monthValue - startDate.monthValue).toLong()

        // If today's day is before start day, that partial month hasn't completed
        val adjustedMonths = max(0L, if (today.dayOfMonth < startDate.dayOfMonth) totalMonths - 1L else totalMonths)

        // How many full intervals have passed since start
        val intervalsPassed = adjustedMonths / intervalMonths

        // Last payment date that is <= today (respecting end-of-month rules of plusMonths)
        val lastPayment = startDate.plusMonths(intervalsPassed * intervalMonths)

        // If lastPayment is after today (can happen with adjustments), step back one interval
        val normalizedLast = if (lastPayment.isAfter(today)) lastPayment.minusMonths(intervalMonths) else lastPayment

        // If today is exactly a payment date, return next; otherwise return next after normalizedLast
        return if (!today.isBefore(normalizedLast)) {
            normalizedLast.plusMonths(intervalMonths)
        } else {
            normalizedLast
        }
    }

    fun Double.roundToTwoDecimals(): Double {
        val bd = BigDecimal(this).setScale(2, RoundingMode.HALF_UP)
        return if (bd.compareTo(BigDecimal.ZERO) == 0) 0.0 else bd.toDouble()
    }
}