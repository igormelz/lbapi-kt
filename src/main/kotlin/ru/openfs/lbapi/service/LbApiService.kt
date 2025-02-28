package ru.openfs.lbapi.service

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.service.adapter.LbCoreSoapAdapter

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

    fun getRecoveryPassword(login: String): List<SoapRecoverPassword> =
        adapter.getResponseAsMandatoryType(
            null,
            RecoverPassword().apply {
                this.login = login
                this.transport = 0 // email
                this.isSimplerecover = false
                this.isSync = false
            },
            RecoverPasswordResponse::class.java
        ).second.ret

    fun updatePasswordByCode(login: String, password: String, code: String): String? =
        adapter.getResponseAsMandatoryType(
            null,
            UpdatePasswordByCode().apply {
                this.`val` = SoapUpdatePasswordByCode().apply {
                    this.login = login
                    this.password = password
                    this.acceptcode = code
                }
            },
            UpdatePasswordByCodeResponse::class.java
        ).second.ret

    fun updatePasswordByToken(password: String, token: String): String? =
        adapter.getResponseAsMandatoryType(
            null,
            UpdatePasswordByToken().apply {
                this.`val` = SoapUpdatePasswordByToken().apply {
                    this.password = password
                    this.token = token
                }
            },
            UpdatePasswordByTokenResponse::class.java
        ).second.ret

}