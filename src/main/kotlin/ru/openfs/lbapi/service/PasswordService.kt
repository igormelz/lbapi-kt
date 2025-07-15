package ru.openfs.lbapi.service

import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.exception.ApiException
import ru.openfs.lbapi.model.PasswordTemplate
import ru.openfs.lbapi.service.adapter.EmailAdapter

@ApplicationScoped
class PasswordService(
    private val emailAdapter: EmailAdapter,
    private val clientService: SoapClientService,
) {

    fun getAccountIsEmailConfirm(login: String): Boolean =
        clientService.withSession().request<SoapInfoAboutAccountDataConfirmResponse> {
            GetInfoAboutAccountDataConfirm().apply {
                this.accountlogin = login
            }
        }.isEmailisconfirmed

    fun getRecoveryPassword(login: String): SoapRecoverPassword =
        clientService.withSession().request<RecoverPasswordResponse> {
            RecoverPassword().apply {
                this.login = login
                this.transport = 0 // email
                this.isSimplerecover = false
                this.isSync = false
            }
        }.ret.first()

    fun updatePasswordByCode(login: String, password: String, code: String): String? =
        clientService.withSession().request<UpdatePasswordByCodeResponse> {
            UpdatePasswordByCode().apply {
                this.`val` = SoapUpdatePasswordByCode().apply {
                    this.login = login
                    this.password = password
                    this.acceptcode = code
                }
            }
        }.ret

    fun updatePasswordByToken(password: String, token: String): String? =
        clientService.withSession().request<UpdatePasswordByTokenResponse> {
            UpdatePasswordByToken().apply {
                this.`val` = SoapUpdatePasswordByToken().apply {
                    this.password = password
                    this.token = token
                }
            }
        }.ret

    private fun getOptionByName(sessionId: String, optionName: String? = "pass_length"): SoapOption? =
        clientService.withSession(sessionId).request<GetOptionByNameResponse> {
            GetOptionByName().apply {
                this.name = optionName
            }
        }.ret.firstOrNull()

    fun getPassTemplate(sessionId: String): PasswordTemplate {
        if (getOptionByName(sessionId)?.availabilitytype?.toInt() != 0) throw ApiException("not allowed")

        return clientService.withSession(sessionId).request<GetPassTemplatesResponse> {
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
        clientService.withSession(sessionId).request<UpdClientPassResponse> {
            UpdClientPass().apply {
                this.oldpass = oldPass
                this.newpass = newPass
            }
        }.ret == 1L

    fun sentEmailConfirmed(): Uni<String> {
        return emailAdapter.sendEmail("TEST")
    }
}