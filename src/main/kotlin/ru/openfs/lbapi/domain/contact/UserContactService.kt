package ru.openfs.lbapi.domain.contact

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import ru.openfs.lbapi.api3.*

@ApplicationScoped
class UserContactService(
    private val soapAdapter: SoapAdapter,
) {

    fun isEmailReady(sessionId: String): Boolean =
        soapAdapter.withSession(sessionId).request<EmailIsReadyResponse> {
            EmailIsReady()
        }.isRet

    fun getAccountIsEmailConfirm(login: String): Boolean =
        soapAdapter.noSession().request<SoapInfoAboutAccountDataConfirmResponse> {
            GetInfoAboutAccountDataConfirm().apply {
                this.accountlogin = login
            }
        }.isEmailisconfirmed

    fun validateEmail(sessionId: String): Long =
        soapAdapter.withSession(sessionId).request<ValidateClientEmailResponse> {
            ValidateClientEmail()
        }.ret

    fun confirmValidateEmail(sessionId: String, recordId: Long, code: String): Boolean =
        soapAdapter.withSession(sessionId).request<ConfirmValidateClientEmailResponse> {
            ConfirmValidateClientEmail().apply {
                this.id = recordId
                this.code = code
            }
        }.ret == 1L


    fun upsertEmail(sessionId: String, email: String): Boolean =
        soapAdapter.withSession(sessionId).request<SetClientInfoResponse> {
            SetClientInfo().apply {
                this.`val` = SoapClientInfo().apply {
                    this.email = email
                }
            }
        }.ret == 1L

    fun upsertMobile(sessionId: String, mobile: String): Boolean =
        soapAdapter.withSession(sessionId).request<SetClientInfoResponse> {
            SetClientInfo().apply {
                this.`val` = SoapClientInfo().apply {
                    this.mobile = mobile
                }
            }
        }.ret == 1L
}