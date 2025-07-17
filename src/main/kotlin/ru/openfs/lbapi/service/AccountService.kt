package ru.openfs.lbapi.service

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.GetClientAccount
import ru.openfs.lbapi.api3.GetClientAccountResponse
import ru.openfs.lbapi.api3.SoapAccountFull
import ru.openfs.lbapi.api3.SoapGetAccountFilter
import ru.openfs.lbapi.service.adapter.SoapAdapter

@ApplicationScoped
class AccountService(
    private val soapAdapter: SoapAdapter,
) {
    fun getAccounts(sessionId: String): SoapAccountFull =
        soapAdapter.withSession(sessionId).request<GetClientAccountResponse> {
            GetClientAccount().apply {
                this.flt = SoapGetAccountFilter().apply {
                    this.activonly = 1L
                }
            }
        }.ret.first()
}