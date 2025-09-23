package ru.openfs.lbapi.domain.agreement.model

import io.vertx.mutiny.sqlclient.Row
import ru.openfs.lbapi.domain.blocking.model.UserBlockSchedule

data class AgreementInfo(
    val id: Long,
    val number: String,
    val createDate: String,
    val balance: Double,
    val recPaymentAmount: Double,
    val promiseCreditAmount: Double,
    val isCredit: Boolean,
    val creditLimitAmount: Double,
    val serviceInfo: ServiceInfo?,
    val promiseCredit: PromiseCredit?,
    val activeUserBlockSchedule: UserBlockSchedule?,
)

data class PromiseCredit(
    val amount: Double,
    val untilDate: String?,
    val isActive: Boolean,
)

data class RentByPeriod(
    val rentPeriod: String,
    val rentAmount: Double,
)

data class ServiceInfo(
    val id: Long,
    val login: String,
    val address: String,
    val tarType: String,
    val tarName: String,
    val tarShape: String,
    val tarRent: Double,
    val blocked: Boolean,
    val blockedType: Long,
    val rentPeriod: String,
    val extService: List<ExtService>,
    val rentSummary: List<RentByPeriod>
)

data class ExtService(
    val descr: String,
    val rent: Double,
    val rentPeriod: String,
    val state: Long,
    val stateDescr: String
) {
    companion object {
        fun fromRow(row: Row): ExtService? {
            try {
                return ExtService(
                    descr = row.getString("descr"),
                    rent = row.getDouble("above"),
                    rentPeriod = when (row.getInteger("rent_period")) {
                        0 -> "разовое"
                        1 -> "в мес."
                        2 -> "в день"
                        3 -> "ежедневное равными долями"
                        4 -> "в год"
                        else -> "NaN"
                    },
                    state = row.getLong("state"),
                    stateDescr = when (row.getInteger("state")) {
                        0, 2 -> "отключена"
                        else -> "включена"
                    }
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
}