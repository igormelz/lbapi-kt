package ru.openfs.lbapi.service

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.ClientPromisePayment
import ru.openfs.lbapi.api3.ClientPromisePaymentResponse
import ru.openfs.lbapi.api3.GetClientPPSettings
import ru.openfs.lbapi.api3.GetClientPPSettingsResponse
import ru.openfs.lbapi.model.PromiseSettings
import java.time.LocalDate

@ApplicationScoped
class PromisePaymentService(
    private val clientService: SoapClientService,
) {

    fun getClientPromiseSettings(sessionId: String, agreementId: Long): PromiseSettings {
        return clientService.withSession(sessionId).request<GetClientPPSettingsResponse> {
            GetClientPPSettings().apply { this.agrm = agreementId }
        }.ret.first().let {
            PromiseSettings(
                isAllowed = it.promiseavailable == 1L,
                dateLimit = LocalDate.now().plusDays(it.promisetill),
                maxAmount = it.promisemax,
                minAmount = it.promisemin,
                limitAmount = it.promiselimit
            )
        }
    }

    fun promisePayment(sessionId: String, agreementId: Long, amount: Double): Boolean =
        clientService.withSession(sessionId).request<ClientPromisePaymentResponse> {
            ClientPromisePayment().apply {
                agrm = agreementId
                summ = amount
            }
        }.ret == 1L

}