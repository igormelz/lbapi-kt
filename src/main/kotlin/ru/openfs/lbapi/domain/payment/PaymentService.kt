package ru.openfs.lbapi.domain.payment

import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.common.exception.PaymentAmountLessRequiredException
import ru.openfs.lbapi.common.exception.PaymentException
import ru.openfs.lbapi.common.utils.FormatUtil.getDateTimeString
import ru.openfs.lbapi.domain.payment.model.PaymentConfirmation
import ru.openfs.lbapi.domain.payment.model.PaymentInfo
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import ru.openfs.yookassa.checkout.service.YookassaCheckoutService
import ru.openfs.yookassa.checkout.service.model.YookassaCheckoutRequest
import ru.openfs.yookassa.checkout.service.model.YookassaCheckoutType
import java.time.LocalDate
import java.time.Month

@ApplicationScoped
class PaymentService(
    private val soapAdapter: SoapAdapter,
    @param:ConfigProperty(name = "payment-success-url") private val successUrl: String,
    @param:ConfigProperty(name = "payment-description") private val descriptionTemplate: String,
    private val yookassaCheckoutService: YookassaCheckoutService,
) {

    fun getClientPayments(sessionId: String, agreementId: Long, year: Int?): List<PaymentInfo> =
        soapAdapter.withSession(sessionId).request<GetClientPaymentsResponse> {
            GetClientPayments().apply {
                this.flt = SoapFilter().apply {
                    this.agrmid = agreementId
/*
                    this.dtfrom = year?.let {
                        LocalDate.of(it, Month.JANUARY, 1).toString()
                    } ?: LocalDate.of(LocalDate.now().year, Month.JANUARY, 1).toString()
                    this.dtto = year?.let {
                        LocalDate.of(it, Month.DECEMBER, 31).plusDays(1L).toString()
                    } ?: LocalDate.now().plusDays(1L).toString()
*/
                }
            }
        }.ret.take(10).map {
            PaymentInfo(
                paymentType = it.mgr,
                paymentCode = it.pay.comment,
                paymentId = it.pay.receipt,
                paymentAmount = it.amountcurr,
                paymentDate = it.pay.paydate
            )
        }

    fun payment(
        sessionId: String,
        agreementId: Long,
        agreementNumber: String,
        amount: Double,
        sbp: Boolean
    ): PaymentConfirmation {
        Log.info("try payment for [$sessionId], agreement: [$agreementNumber], amount: [$amount]")
        validateAmount(amount)

        return createOrderNumber(sessionId, agreementId, agreementNumber, amount)
            .let {
                Log.info("success create order for [$sessionId], agreement: [$agreementNumber], amount: [$amount], order:[$it]")
                yookassaCheckoutService.checkout(
                    YookassaCheckoutRequest(
                        if (sbp) YookassaCheckoutType.SBP else YookassaCheckoutType.BANK_CARD,
                        it.toString(),
                        amount,
                        successUrl,
                        createDescription(it, agreementNumber),
                    )
                )
            }.let {
                if (it.success) {
                    Log.info("success create payment for [$sessionId], agreement: [$agreementNumber], amount: [$amount], paymentId:[${it.data.paymentId}]")
                    PaymentConfirmation(it.data.confirmUrl, it.data.paymentId)
                } else {
                    Log.error("error payment for [$sessionId], agreement: [$agreementNumber], amount: [$amount]: ${it.error}")
                    throw PaymentException(it.error)
                }
            }
    }

    fun getPayment(sessionId: String, paymentId: String): String {
        Log.info("try get payment for [$sessionId]")
        return yookassaCheckoutService.getStatus(paymentId).let {
            if (it.success) {
                Log.info("payment for [$sessionId]: ${it.data}")
                it.data
            } else {
                Log.error("error payment for [$sessionId]: ${it.error}")
                throw PaymentException(it.error)
            }
        }
    }

    private fun validateAmount(amount: Double): Boolean =
        if (amount <= 0) throw PaymentAmountLessRequiredException("required > 1")
        else if (amount > 20000) throw PaymentAmountLessRequiredException("required < 20000")
        else true

    private fun createOrderNumber(sessionId: String, agreementId: Long, agreementNumber: String, amount: Double): Long =
        soapAdapter.withSession(sessionId).request<InsPrePaymentResponse> {
            InsPrePayment().apply {
                this.`val` = SoapPrePayment().apply {
                    this.agrmid = agreementId
                    this.amount = amount
                    this.curname = "RUR"
                    this.paydate = getDateTimeString()
                    this.comment = "ЛК"
                }
            }
        }.ret

    private fun createDescription(orderNumber: Long, agreementNumber: String): String =
        descriptionTemplate
            .replace("{orderNumber}", orderNumber.toString())
            .replace("{agreementNumber}", agreementNumber)

}