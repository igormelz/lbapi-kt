package ru.openfs.lbapi.domain.petrocom.model

import ru.openfs.lbapi.domain.account.model.AccountDto

data class PetrocomFirstLogin(
    val sessionId: String,
    val needAction: Boolean,
    val account: AccountDto
)
