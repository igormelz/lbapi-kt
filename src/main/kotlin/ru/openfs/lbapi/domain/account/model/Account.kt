package ru.openfs.lbapi.domain.account.model

data class Account(
    val login: String,
    val uid: Long,
    val name: String,
    val surname: String?,
    val patronymic: String?,
    val email: String?,
    val mobile: String?,
    val emailVerified: Boolean,
    val mobileVerified: Boolean,
)
