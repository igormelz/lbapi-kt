package ru.openfs.lbapi.model

data class ClientVGroup(
    val vgId: Long,
    val login: String,
    val tarId: Long,
    val descr: String,
)
