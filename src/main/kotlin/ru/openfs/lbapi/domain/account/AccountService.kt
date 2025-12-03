package ru.openfs.lbapi.domain.account

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.GetClientAccount
import ru.openfs.lbapi.api3.GetClientAccountResponse
import ru.openfs.lbapi.api3.SoapAccountFull
import ru.openfs.lbapi.api3.SoapGetAccountFilter
import ru.openfs.lbapi.common.exception.NeedReAuthorizeException
import ru.openfs.lbapi.domain.account.model.Account
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter

@ApplicationScoped
class AccountService(
    private val soapAdapter: SoapAdapter,
) {

    private val clientAccount: GetClientAccount by lazy(LazyThreadSafetyMode.PUBLICATION) {
        GetClientAccount().apply {
            flt = SoapGetAccountFilter().apply {
                activonly = 1L
            }
        }
    }

    fun getClientAccount(sessionId: String, login: String?): SoapAccountFull =
        soapAdapter.withSession(sessionId)
            .request<GetClientAccountResponse> { clientAccount }.ret.first().let {
                if(!login.isNullOrBlank() && it.account.login != login) {
                    throw NeedReAuthorizeException("login [$login] diff than [${it.account.login}], session:[$sessionId]")
                } else it
            }

    fun getAccount(sessionId: String, login: String?): Account =
        mapAccountFromApi(getClientAccount(sessionId, login))

    private fun mapAccountFromApi(soap: SoapAccountFull): Account =
        Account(
            uid = soap.account.uid,
            login = soap.account.login,
            name = soap.account.abonentname,
            surname = soap.account.abonentsurname,
            patronymic = soap.account.abonentpatronymic,
            email = soap.account.email,
            emailVerified = soap.account.isEmailisconfirmed,
            mobile = soap.account.mobile,
            mobileVerified = soap.account.isMobileisconfirmed,
        )
}