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

    fun closeSession(sessionId: String) =
        adapter.getResponseAsString(sessionId, Logout())

    fun getAccounts(sessionId: String) =
        adapter.getResponse(
            sessionId,
            GetClientAccount().apply {
                this.flt = SoapGetAccountFilter().apply {
                    this.activonly = 1L
                }
            },
            GetClientAccountResponse::class.java
        ).second

}