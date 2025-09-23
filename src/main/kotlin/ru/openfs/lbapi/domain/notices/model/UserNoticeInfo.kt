package ru.openfs.lbapi.domain.notices.model

@JvmRecord
data class UserNoticeInfo(
    val email: Boolean,
    val type: Long,
    val description: String,
    val value: String?,
)