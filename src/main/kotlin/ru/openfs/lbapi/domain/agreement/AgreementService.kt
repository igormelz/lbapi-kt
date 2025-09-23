package ru.openfs.lbapi.domain.agreement

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.infrastructure.adapter.DbAdapter
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import ru.openfs.lbapi.api3.GetClientAccount
import ru.openfs.lbapi.api3.GetClientAccountResponse
import ru.openfs.lbapi.api3.GetClientPromisePayments
import ru.openfs.lbapi.api3.GetClientPromisePaymentsResponse
import ru.openfs.lbapi.api3.GetClientStat
import ru.openfs.lbapi.api3.GetClientStatResponse
import ru.openfs.lbapi.api3.GetRecommendedPayment
import ru.openfs.lbapi.api3.GetRecommendedPaymentResponse
import ru.openfs.lbapi.api3.GetVgUserBlockSchedule
import ru.openfs.lbapi.api3.GetVgUserBlockScheduleResponse
import ru.openfs.lbapi.api3.SoapFilter
import ru.openfs.lbapi.api3.SoapGetAccountFilter
import ru.openfs.lbapi.api3.SoapGetVgUserBlockSchedule
import ru.openfs.lbapi.api3.SoapStat
import ru.openfs.lbapi.domain.agreement.model.AgreementInfo
import ru.openfs.lbapi.domain.agreement.model.InvoiceData
import ru.openfs.lbapi.domain.agreement.model.PromiseCredit
import ru.openfs.lbapi.domain.blocking.model.UserBlockSchedule
import ru.openfs.lbapi.common.utils.FormatUtil
import ru.openfs.lbapi.common.utils.FormatUtil.isDateTimeAfterNow
import java.time.LocalDate

@ApplicationScoped
class AgreementService(
    private val dbAdapter: DbAdapter,
    private val soapAdapter: SoapAdapter
) {
    fun getAgreementsInfo(sessionId: String): List<AgreementInfo> =
        soapAdapter.withSession(sessionId).request<GetClientAccountResponse> {
            GetClientAccount().apply {
                this.flt = SoapGetAccountFilter().apply {
                    this.activonly = 1L
                }
            }
        }.ret.first().agreements.map { agreement ->
            val serviceInfo = dbAdapter.getVGroupsAndServices(agreement.agrmid)

            AgreementInfo(
                id = agreement.agrmid,
                number = agreement.number,
                createDate = agreement.date,
                balance = agreement.balance,
                recPaymentAmount = getRecommendedPayment(sessionId, agreement.agrmid),
                promiseCreditAmount = agreement.promisecredit,
                isCredit = agreement.paymentmethod == 1L,
                creditLimitAmount = agreement.credit,
                serviceInfo = serviceInfo,
                promiseCredit = getPromiseCredit(sessionId, agreement.agrmid).takeIf { agreement.promisecredit != 0.0 },
                activeUserBlockSchedule = getActiveUserBlockSchedule(sessionId, agreement.agrmid, serviceInfo?.id)
            )
        }

    private fun getPromiseCredit(sessionId: String, agreementId: Long): PromiseCredit? =
        soapAdapter.withSession(sessionId).request<GetClientPromisePaymentsResponse> {
            GetClientPromisePayments().apply {
                this.flt = SoapFilter().apply {
                    this.agrmid = agreementId
                    this.dtfrom = LocalDate.now().minusMonths(1L).toString()
                    this.dtto = LocalDate.now().plusMonths(1L).toString()
                }
            }
        }.ret.firstOrNull()?.let {
            PromiseCredit(
                amount = it.amount,
                untilDate = it.promtill,
                isActive = it.status == 3.toShort()
            )
        }

    private fun getActiveUserBlockSchedule(sessionId: String, agreementId: Long, vgId: Long?): UserBlockSchedule? {
        if (vgId == null) return null
        return soapAdapter.withSession(sessionId).request<GetVgUserBlockScheduleResponse> {
            GetVgUserBlockSchedule().apply {
                this.flt = SoapGetVgUserBlockSchedule().apply {
                    this.agrmid = agreementId
                    this.vgid = vgId
                }
            }
        }.ret.firstOrNull() { it.timeto.isDateTimeAfterNow() }?.let {
            UserBlockSchedule(
                it.recordid,
                it.creationdate,
                it.timefrom,
                it.timeto,
                true
            )
        }
    }

    fun getClientTarStat(
        sessionId: String,
        agreementId: Long,
        vgId: Long,
        dateFrom: String? = null,
        dateTo: String? = null
    ): SoapStat =
        soapAdapter.withSession(sessionId).request<GetClientStatResponse> {
            GetClientStat().apply {
                this.flt = SoapFilter().apply {
                    this.dtfrom = dateFrom ?: FormatUtil.getDateStartMonth()
                    this.dtto = dateTo ?: FormatUtil.getDateStartNextMonth()
                    this.agrmid = agreementId
                    this.vgid = vgId
                    this.repnum = 14529L
                    this.repdetail = 0L //?
                }
            }
        }.ret.first()


    fun getClientSrvStat(
        sessionId: String,
        agreementId: Long,
        vgId: Long,
        dateFrom: String? = null,
        dateTo: String? = null
    ): SoapStat =
        soapAdapter.withSession(sessionId).request<GetClientStatResponse> {
            GetClientStat().apply {
                this.flt = SoapFilter().apply {
                    this.dtfrom = dateFrom ?: FormatUtil.getDateStartMonth()
                    this.dtto = dateTo ?: FormatUtil.getDateStartNextMonth()
                    this.agrmid = agreementId
                    this.vgid = vgId
                    this.repnum = 3L
                    this.repdetail = 1L
                }
            }
        }.ret.first()


    fun getAgreementStat(sessionId: String, agreementId: Long): List<InvoiceData> =
        dbAdapter.getVGroups(agreementId).flatMap { vGroup ->
            getClientTarStat(sessionId, agreementId, vGroup.vgId)
                .data.map { v ->
                    InvoiceData(
                        // vg names: [uid, agrm_id, vg_id, period, tar_id, rent, serv, above, pays]
                        vgId = v.`val`[2].toLong(),
                        tarId = v.`val`[4].toLong(),
                        serviceName = vGroup.descr,
                        //period = v.`val`[3],
                        charges = v.`val`[5].toDouble() + v.`val`[6].toDouble() + v.`val`[7].toDouble(),
                    )
                } + getClientSrvStat(sessionId, agreementId, vGroup.vgId)
                .data.map { v ->
                    InvoiceData(
                        // srv names: [agent_descr, agent_id, agent_type, agrm_id, amount, ani, cat_idx, charge_flag, curr_symbol, dst_ip, dst_port, duration, src_ip, tar_id, total, vg_id, volume, volume_in, volume_out, zone_descr]
                        vgId = v.`val`[15].toLong(),
                        tarId = v.`val`[13].toLong(),
                        serviceName = v.`val`[19],
                        charges = v.`val`[4].toDouble(),
                    )
                }.filter { it.charges > 0 }
        }


    private fun getRecommendedPayment(sessionId: String, agreementId: Long, mode: Long = 3L): Double =
        soapAdapter.withSession(sessionId).request<GetRecommendedPaymentResponse> {
            GetRecommendedPayment().apply {
                this.id = agreementId
                this.mode = mode
                this.isConsiderinstallment = true
            }
        }.ret

}