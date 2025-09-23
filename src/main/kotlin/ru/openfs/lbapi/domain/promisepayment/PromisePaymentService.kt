package ru.openfs.lbapi.domain.promisepayment

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import ru.openfs.lbapi.api3.ClientPromisePayment
import ru.openfs.lbapi.api3.ClientPromisePaymentResponse
import ru.openfs.lbapi.api3.GetClientPPSettings
import ru.openfs.lbapi.api3.GetClientPPSettingsResponse
import ru.openfs.lbapi.domain.promisepayment.model.PromiseSettings
import java.time.LocalDate

@ApplicationScoped
class PromisePaymentService(
    private val soapAdapter: SoapAdapter,
) {

    fun getClientPromiseSettings(sessionId: String, agreementId: Long): PromiseSettings {
        return soapAdapter.withSession(sessionId).request<GetClientPPSettingsResponse> {
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
        soapAdapter.withSession(sessionId).request<ClientPromisePaymentResponse> {
            ClientPromisePayment().apply {
                agrm = agreementId
                summ = amount
            }
        }.ret == 1L

}