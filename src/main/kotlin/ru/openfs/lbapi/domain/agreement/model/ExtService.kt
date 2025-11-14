package ru.openfs.lbapi.domain.agreement.model

import io.quarkus.logging.Log
import io.vertx.mutiny.sqlclient.Row
import ru.openfs.lbapi.common.utils.FormatUtil.nextPaymentDate
import java.time.LocalDate
import kotlin.math.max

data class ExtService(
    val descr: String,
    val rent: Double,
    val rentPeriod: String,
    val state: Long,
    val stateDescr: String,
    val nextPayDate: String?, // just for
) {
    companion object {
        fun fromRow(row: Row): ExtService? {
            return try {
                if (row.getInteger("need_calc") == 0 || row.getInteger("state") != 3) {
                    null
                } else {
                    val rp = row.getInteger("rent_period")
                    val rpm = row.getInteger("rent_period_month")
                    ExtService(
                        descr = row.getString("descr"),
                        rent = row.getDouble("above"),
                        rentPeriod = when (rp) {
                            0 -> "разовое"
                            1 -> "в мес."
                            2 -> "в день"
                            3 -> "ежедневное равными долями"
                            4 -> if (rpm == 12) "в год" else "за 6 мес."
                            else -> "NaN"
                        },
                        state = row.getLong("state"),
                        stateDescr = when (row.getInteger("state")) {
                            0, 2 -> "отключена"
                            else -> "включена"
                        },
                        nextPayDate = if (rp == 4) {
                            when (row.getInteger("begin_period")) {
                                // 0 -> FormatUtil.getDateStartNextMonth()
                                // 1 -> "договор"
                                // 2 -> activate.withYear(LocalDate.now().year).toString()
                                2 -> nextPaymentDate(
                                    startDate = row.getLocalDate("activated"),
                                    intervalMonths = rpm.toLong()
                                ).toString()
                                // 3 -> "подключение"
                                3 -> nextPaymentDate(
                                    startDate = row.getLocalDate("timefrom"),
                                    intervalMonths = rpm.toLong()
                                ).toString()

                                else -> ""
                            }
                        } else ""
                    )
                }
            } catch (e: Exception) {
                Log.warn("Exception while parsing ExtService; ${e.message}")
                null
            }
        }
    }
}
