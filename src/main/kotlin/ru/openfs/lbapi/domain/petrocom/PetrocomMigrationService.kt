package ru.openfs.lbapi.domain.petrocom

import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import ru.openfs.lbapi.api3.BlkVgroup
import ru.openfs.lbapi.api3.BlkVgroupResponse
import ru.openfs.lbapi.domain.account.AccountService
import ru.openfs.lbapi.domain.agreement.AgreementService
import ru.openfs.lbapi.domain.petrocom.model.PetrocomFirstLogin
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter

@ApplicationScoped
class PetrocomMigrationService(
    private val soapAdapter: SoapAdapter,
    @param:ConfigProperty(name = "manager") private val manager: String,
    @param:ConfigProperty(name = "password") private val password: String,
    private val agreementService: AgreementService,
    private val accountService: AccountService,
) {

    fun loginFirst(login: String, pass: String): PetrocomFirstLogin {
        return soapAdapter.startClientSession(login, pass).let { sessionId ->
            PetrocomFirstLogin(
                sessionId = sessionId,
                needAction = agreementService.getAgreements(sessionId, login).all { agreement ->
                    agreementService.getAgreementInfo(
                        sessionId, login, agreement.number
                    ).serviceInfo?.blockedType == 10L
                },
                account = accountService.getAccount(sessionId, login)
            )
        }
    }

    fun agree(sessionId: String, login: String): Boolean {
        var answer = false
        agreementService.getAgreements(sessionId, login).forEach { agreement ->
            val service = agreementService.getAgreementInfo(sessionId, login, agreement.number).serviceInfo
            if (service?.blockedType == 10L) {
                Log.info("try off blocking for ${agreement.number} and vgid=${service.id}")
                answer = managerAction(service.id)
            }
        }
        return answer
    }

    private fun managerAction(vgId: Long): Boolean {
        val mSessionId = soapAdapter.startServiceSession(manager, password)
        Log.info("logged as manager:[$mSessionId]")
        return soapAdapter.withSession(mSessionId).request<BlkVgroupResponse> {
            BlkVgroup().apply {
                id = vgId
                blk = 10L
                state = "off"
                comment = "switch by agree"
            }
        }.ret == 1L
    }

}