package ru.openfs.lbapi.domain.agreement.model

data class ClientVGroup(
    val vgId: Long,
    val login: String,
    val tarId: Long,
    val descr: String,
)