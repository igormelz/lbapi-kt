package ru.openfs.lbapi.domain.account

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import ru.openfs.lbapi.api3.GetClientAccount
import ru.openfs.lbapi.api3.GetClientAccountResponse
import ru.openfs.lbapi.api3.SoapGetAccountFilter
import ru.openfs.lbapi.domain.account.model.Account

@ApplicationScoped
class AccountService(
    private val soapAdapter: SoapAdapter,
) {
    fun getAccount(sessionId: String): Account {
        return soapAdapter.withSession(sessionId).request<GetClientAccountResponse> {
            GetClientAccount().apply {
                this.flt = SoapGetAccountFilter().apply {
                    this.activonly = 1L
                }
            }
        }.ret.first().let {
            Account(
                uid = it.account.uid,
                login = it.account.login,
                name = it.account.abonentname,
                surname = it.account.abonentsurname,
                patronymic = it.account.abonentpatronymic,
                email = it.account.email,
                emailVerified = it.account.isEmailisconfirmed,
                mobile = it.account.mobile,
                mobileVerified = it.account.isMobileisconfirmed,
            )
        }
    }
}