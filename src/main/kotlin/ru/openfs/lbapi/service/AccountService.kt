package ru.openfs.lbapi.service

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.GetClientAccount
import ru.openfs.lbapi.api3.GetClientAccountResponse
import ru.openfs.lbapi.api3.SoapAccountFull
import ru.openfs.lbapi.api3.SoapGetAccountFilter

@ApplicationScoped
class AccountService(
    private val clientService: SoapClientService,
) {
    fun getAccounts(sessionId: String): SoapAccountFull =
        clientService.withSession(sessionId).request<GetClientAccountResponse> {
            GetClientAccount().apply {
                this.flt = SoapGetAccountFilter().apply {
                    this.activonly = 1L
                }
            }
        }.ret.first()
}