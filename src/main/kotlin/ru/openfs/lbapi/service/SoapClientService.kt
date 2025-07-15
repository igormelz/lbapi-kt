package ru.openfs.lbapi.service

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.service.adapter.SoapAdapter

@ApplicationScoped
class SoapClientService(
    private val adapter: SoapAdapter
) {

    fun withSession(sessionId: String? = null): SessionRequestBuilder {
        return SessionRequestBuilder(adapter, sessionId)
    }

    fun startSession(login: String, password: String): String = adapter.loginClient(login, password)
}