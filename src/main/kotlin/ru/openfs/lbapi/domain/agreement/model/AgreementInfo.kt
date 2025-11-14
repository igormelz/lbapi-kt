package ru.openfs.lbapi.domain.agreement.model

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
    val changeTariff: ChangeTariff?,
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