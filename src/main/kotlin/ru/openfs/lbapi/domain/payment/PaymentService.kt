package ru.openfs.lbapi.domain.payment

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.GetClientPayments
import ru.openfs.lbapi.api3.GetClientPaymentsResponse
import ru.openfs.lbapi.api3.SoapFilter
import ru.openfs.lbapi.domain.payment.model.PaymentInfo
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import java.time.LocalDate
import java.time.Month

@ApplicationScoped
class PaymentService(
    private val soapAdapter: SoapAdapter
) {

    fun getClientPayments(sessionId: String, agreementId: Long, year: Int? = null): List<PaymentInfo> =
        soapAdapter.withSession(sessionId).request<GetClientPaymentsResponse> {
            GetClientPayments().apply {
                this.flt = SoapFilter().apply {
                    this.agrmid = agreementId
                    this.dtfrom = if (year == null)
                        LocalDate.of(LocalDate.now().year, Month.JANUARY, 1).toString()
                    else
                        LocalDate.of(year, Month.JANUARY, 1).toString()
                    this.dtto = if (year == null)
                        LocalDate.now().plusDays(1L).toString()
                    else
                        LocalDate.of(year, Month.DECEMBER, 31).plusDays(1L).toString()
                }
            }
        }.ret.map {
            PaymentInfo(
                paymentType = it.mgr,
                paymentCode = it.pay.comment,
                paymentId = it.pay.receipt,
                paymentAmount = it.amountcurr,
                paymentDate = it.pay.paydate
            )
        }

}