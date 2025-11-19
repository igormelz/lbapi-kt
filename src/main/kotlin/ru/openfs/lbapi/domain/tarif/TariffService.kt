package ru.openfs.lbapi.domain.tarif

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter

@ApplicationScoped
class TariffService(
    private val soapAdapter: SoapAdapter,
) {

    fun getTariffsHistory(sessionId: String, agreementId: Long): List<SoapTarifsHistory> {
        return soapAdapter.withSession(sessionId).request<GetTarifsHistoryResponse> {
            GetTarifsHistory().apply {
                this.flt = SoapFilter().apply {
                    this.agrmid = agreementId
                }
            }
        }.ret
    }
}