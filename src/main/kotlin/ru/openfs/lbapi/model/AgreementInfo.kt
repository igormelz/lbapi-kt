package ru.openfs.lbapi.model

data class AgreementInfo (
    val id: Long,
    val number: String,
    val createDate: String,
    val balance: Double,
    val recPaymentAmount: Double,
    val promiseCreditAmount: Double,
    val isCredit: Boolean,
    val creditLimitAmount: Double,
    val serviceInfo: List<ServiceInfo>?
)