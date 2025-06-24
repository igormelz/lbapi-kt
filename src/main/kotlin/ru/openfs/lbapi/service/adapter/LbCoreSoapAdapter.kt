package ru.openfs.lbapi.service.adapter

import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.apache.camel.ProducerTemplate
import org.eclipse.microprofile.rest.client.inject.RestClient
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.camel.CamelRoute
import ru.openfs.lbapi.client.LbCoreRestClient
import ru.openfs.lbapi.exception.ApiException
import ru.openfs.lbapi.exception.NotAuthorizeException
import ru.openfs.lbapi.exception.NotfoundAccountException
import ru.openfs.lbapi.exception.PromisePaymentNotAllowedException
import ru.openfs.lbapi.exception.PromisePaymentOverdueException
import ru.openfs.lbapi.utils.FormatUtil.getDateStartMonth
import ru.openfs.lbapi.utils.FormatUtil.getDateStartNextMonth
import ru.openfs.lbapi.utils.FormatUtil.getTomorrowDate

@ApplicationScoped
class LbCoreSoapAdapter(
    @RestClient private val restClient: LbCoreRestClient,
    private val producer: ProducerTemplate
) {

    fun getClientTarStat(sessionId: String, agreementId: Long, vgId: Long, dateFrom: String? = null, dateTo: String? = null): SoapStat {
        return getResponseAsMandatoryType(
            sessionId,
            GetClientStat().apply {
                this.flt = SoapFilter().apply {
                    this.dtfrom = dateFrom ?: getDateStartMonth()
                    this.dtto = dateTo ?: getDateStartNextMonth()
                    this.agrmid = agreementId
                    this.vgid = vgId
                    this.repnum = 14529L
                    this.repdetail = 0L //?
                }
            },
            GetClientStatResponse::class.java
        ).second.ret.first()
    }

    fun getClientSrvStat(sessionId: String, agreementId: Long, vgId: Long, dateFrom: String? = null, dateTo: String? = null): SoapStat {
        return getResponseAsMandatoryType(
            sessionId,
            GetClientStat().apply {
                this.flt = SoapFilter().apply {
                    this.dtfrom = dateFrom ?: getDateStartMonth()
                    this.dtto = dateTo ?: getDateStartNextMonth()
                    this.agrmid = agreementId
                    this.vgid = vgId
                    this.repnum = 3L
                    this.repdetail = 1L
                }
            },
            GetClientStatResponse::class.java
        ).second.ret.first()
    }

    fun getClientVGroupByAgreement(sessionId: String, agreementId: Long): List<SoapClientVgroupFull> {
        return getResponseAsMandatoryType(
            sessionId,
            // https://www.lanbilling.ru/technical-information/documentation/api/getclientvgroups1/
            GetClientVgroups().apply {
                this.flt = SoapFilter().apply {
                    this.agrmid = agreementId
                }
            },
            // https://www.lanbilling.ru/technical-information/documentation/api/soapclientvgroupfull/
            GetClientVgroupsResponse::class.java
        ).second.ret
    }

    fun getRecommendedPayment(sessionId: String, agreementId: Long, mode: Long): Double {
        return getResponseAsMandatoryType(
            sessionId,
            GetRecommendedPayment().apply {
                this.id = agreementId
                this.mode = mode
                this.isConsiderinstallment = true
            },
            GetRecommendedPaymentResponse::class.java
        ).second.ret
    }

    fun getSessionId(request: Any): String =
        getResponseAsMandatoryType(null, request, String::class.java).first
            ?: throw NotAuthorizeException("not return sessionId")

    fun getResponseAsString(sessionId: String, request: Any): String =
        getResponseAsMandatoryType(sessionId, request, String::class.java).second

    fun <T> getResponseAsMandatoryType(
        sessionId: String?,
        request: Any,
        responseType: Class<T>,
    ): Pair<String?, T> {
        val requestBody = producer.requestBody<String>(
            CamelRoute.Companion.CREATE_SOAP_MESSAGE,
            request,
            String::class.java
        )
        try {
            restClient.call(sessionId, requestBody).use { response ->
                val responseBody = response.readEntity<String>(String::class.java)

                if (response.status != 200) {
                    val err = producer.requestBody(
                        CamelRoute.Companion.PARSE_ERROR_MESSAGE,
                        responseBody,
                        String::class.java
                    )

                    when (err) {
                        "Promise payments already assigned" -> throw PromisePaymentNotAllowedException(err)
                        "Client not authorized" -> throw NotAuthorizeException(err)
                        "Account not found" -> throw NotfoundAccountException(err)
                        "Promise payment is not available, last payment is overdue" -> throw PromisePaymentOverdueException(err)
                        else -> throw ApiException(err)
                    }
                }

                return response.cookies["sessnum"]?.value?.getSessionId() to
                        producer.requestBody<T>(CamelRoute.Companion.READ_SOAP_MESSAGE, responseBody, responseType)
            }
        } catch (e: RuntimeException) {
            Log.error(e.message)
            throw e
        }
    }

    fun getClientVgroupService(sessionId: String, vgId: Long) =
        getResponseAsMandatoryType(
            sessionId,
            GetVgroupServices().apply {
                this.flt = SoapFilter().apply {
                    this.vgid = vgId
                    this.common = 1L
                    this.catidx
                }
            },
            GetVgroupServicesResponse::class.java
        ).second.ret


    fun getUsboxService(sessionId: String, vgId: Long) =
        getResponseAsMandatoryType(
            sessionId,
            GetUsboxServices().apply {
                this.flt = SoapFilter().apply {

                    //this.dtfrom = getDateStartMonth()
                    //this.dtto = getTomorrowDate()

                    // common — периодичность списания услуги:
                    // 0 — разовое
                    // 1 — ежемесячное
                    // 2 — ежедневное
                    // 3 — ежедневное равными долями
                    // 4 — раз в N месяцев
                    // Поле common работает, только если определено поле recordid.
                    // this.common = -1L

                    // showservices — флаг «Только дополнительные услуги»
                    this.showservices = 1L

                    // needcalc — флаг «Необходима тарификация записи»
                    // this.needcalc = 1L

                    // state — текущее состояние услуги:
                    // 0 — отключена, не активирована
                    // 1 — включена, не активирована
                    // 2 — отключена, активирована
                    // 3 — включена, активирована
                    //// this.state = 1L
                    // this.states.addAll(listOf(1,2,3))

                    // servtypes — список типов услуг
                    // 0 — Основная
                    // 1 — Дополнительная
                    // 2 — Автопродляемая
                    // 3 — Абонентская плата
                    //this.servtypes.addAll(listOf(1,3))

                    // this.notusbox = vgid ? -1 if not services : servid
                    // this.unavail = -1L

                    // vgid — идентификатор учётной записи
                    this.vgid = vgId

                    // excludeuuid — значение поля «Код для внешней системы» (услуги с этим кодом будут исключены)
                    this.excludeuuid = "ASIR#"
                }
            },
            GetUsboxServicesResponse::class.java
        ).second.ret.firstOrNull()

    private fun String.getSessionId() = this.split(";").first()

}