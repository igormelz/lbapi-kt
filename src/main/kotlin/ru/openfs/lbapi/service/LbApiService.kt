package ru.openfs.lbapi.service

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*

@ApplicationScoped
class LbApiService(private val adapter: LbCoreSoapAdapter) {

    fun startSession(login: String, password: String) =
        adapter.getSessionId(
            ClientLogin().apply {
                this.login = login
                this.pass = password
            }
        )

    fun closeSession(sessionId: String) = adapter.getResponseAsString(sessionId, Logout())

    fun getAccounts(sessionId: String): List<SoapAccountFull> =
        adapter.getResponseAsMandatoryType(
            sessionId,
            GetClientAccount().apply {
                this.flt = SoapGetAccountFilter().apply {
                    this.activonly = 1L
                }
            },
            GetClientAccountResponse::class.java
        ).second.ret

    fun getAccountIsEmailConfirm(login: String): Boolean =
        adapter.getResponseAsMandatoryType(
            null,
            GetInfoAboutAccountDataConfirm().apply {
                this.accountlogin = login
            },
            SoapInfoAboutAccountDataConfirmResponse::class.java
        ).second.isEmailisconfirmed


}