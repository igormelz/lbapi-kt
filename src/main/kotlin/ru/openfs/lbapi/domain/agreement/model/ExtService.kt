package ru.openfs.lbapi.domain.agreement.model

data class ExtService(
    val descr: String,
    val rent: Double,
    val rentPeriod: String,
    val state: Long,
    val stateDescr: String,
    val nextPayDate: String?, // just for
)
