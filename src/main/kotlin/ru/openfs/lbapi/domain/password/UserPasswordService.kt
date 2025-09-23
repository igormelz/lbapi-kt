package ru.openfs.lbapi.domain.password

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.infrastructure.adapter.EmailAdapter
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import ru.openfs.lbapi.api3.GetOptionByName
import ru.openfs.lbapi.api3.GetOptionByNameResponse
import ru.openfs.lbapi.api3.GetPassTemplates
import ru.openfs.lbapi.api3.GetPassTemplatesResponse
import ru.openfs.lbapi.api3.RecoverPassword
import ru.openfs.lbapi.api3.RecoverPasswordResponse
import ru.openfs.lbapi.api3.SoapFilter
import ru.openfs.lbapi.api3.SoapOption
import ru.openfs.lbapi.api3.SoapRecoverPassword
import ru.openfs.lbapi.api3.SoapUpdatePasswordByCode
import ru.openfs.lbapi.api3.SoapUpdatePasswordByToken
import ru.openfs.lbapi.api3.UpdClientPass
import ru.openfs.lbapi.api3.UpdClientPassResponse
import ru.openfs.lbapi.api3.UpdatePasswordByCode
import ru.openfs.lbapi.api3.UpdatePasswordByCodeResponse
import ru.openfs.lbapi.api3.UpdatePasswordByToken
import ru.openfs.lbapi.api3.UpdatePasswordByTokenResponse
import ru.openfs.lbapi.common.exception.ApiException
import ru.openfs.lbapi.domain.password.model.PasswordTemplate

@ApplicationScoped
class UserPasswordService(
    private val emailAdapter: EmailAdapter,
    private val soapAdapter: SoapAdapter,
) {

    fun getRecoveryPassword(login: String): SoapRecoverPassword =
        soapAdapter.noSession().request<RecoverPasswordResponse> {
            RecoverPassword().apply {
                this.login = login
                this.transport = 0 // email
                this.isSimplerecover = false
                this.isSync = false
            }
        }.ret.first()

    fun updatePasswordByCode(login: String, password: String, code: String): String? =
        soapAdapter.noSession().request<UpdatePasswordByCodeResponse> {
            UpdatePasswordByCode().apply {
                this.`val` = SoapUpdatePasswordByCode().apply {
                    this.login = login
                    this.password = password
                    this.acceptcode = code
                }
            }
        }.ret

    fun updatePasswordByToken(password: String, token: String): String? =
        soapAdapter.noSession().request<UpdatePasswordByTokenResponse> {
            UpdatePasswordByToken().apply {
                this.`val` = SoapUpdatePasswordByToken().apply {
                    this.password = password
                    this.token = token
                }
            }
        }.ret

    private fun getOptionByName(sessionId: String, optionName: String? = "pass_length"): SoapOption? =
        soapAdapter.withSession(sessionId).request<GetOptionByNameResponse> {
            GetOptionByName().apply {
                this.name = optionName
            }
        }.ret.firstOrNull()

    fun getPassTemplate(sessionId: String): PasswordTemplate {
        if (getOptionByName(sessionId)?.availabilitytype?.toInt() != 0) throw ApiException("not allowed")

        return soapAdapter.withSession(sessionId).request<GetPassTemplatesResponse> {
            GetPassTemplates().apply {
                this.flt = SoapFilter().apply {
                    this.objecttype = "account"
                }
            }
        }.ret.firstOrNull()
            .let {
                PasswordTemplate(
                    minLength = it?.minlength ?: 0,
                    hasSpecialChars = it?.needspecial == 1L,
                    hasUppercase = it?.needupper == 1L,
                    hasDigits = it?.neednumbers == 1L,
                    hasLowercase = it?.needlow == 1L,
                    checkPassword = it?.checkpass == 1L,
                    excludeChars = it?.excluded,
                )
            }
    }

    fun updatePassword(sessionId: String, oldPass: String, newPass: String): Boolean =
        soapAdapter.withSession(sessionId).request<UpdClientPassResponse> {
            UpdClientPass().apply {
                this.oldpass = oldPass
                this.newpass = newPass
            }
        }.ret == 1L
}