package ru.openfs.lbapi.domain.blocking

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import ru.openfs.lbapi.api3.DelVgUserBlockSchedule
import ru.openfs.lbapi.api3.DelVgUserBlockScheduleResponse
import ru.openfs.lbapi.api3.GetUserBlockTemplate
import ru.openfs.lbapi.api3.GetUserBlockTemplateResponse
import ru.openfs.lbapi.api3.GetVgUserBlockSchedule
import ru.openfs.lbapi.api3.GetVgUserBlockScheduleResponse
import ru.openfs.lbapi.api3.SetVgUserBlockSchedule
import ru.openfs.lbapi.api3.SetVgUserBlockScheduleResponse
import ru.openfs.lbapi.api3.SoapGetUserBlockTemplate
import ru.openfs.lbapi.api3.SoapGetVgUserBlockSchedule
import ru.openfs.lbapi.api3.SoapVgUserBlockSchedule
import ru.openfs.lbapi.domain.blocking.model.UserBlockSchedule
import ru.openfs.lbapi.domain.blocking.model.UserBlockTemplate
import ru.openfs.lbapi.common.utils.FormatUtil.isDateTimeAfterNow
import java.time.LocalDate

@ApplicationScoped
class BlockingService(
    private val soapAdapter: SoapAdapter,
) {

    fun getUserBlockTemplate(sessionId: String, agreementId: Long, vgId: Long): UserBlockTemplate? =
        soapAdapter.withSession(sessionId).request<GetUserBlockTemplateResponse> {
            GetUserBlockTemplate().apply {
                this.flt = SoapGetUserBlockTemplate().apply {
                    this.agrmid = agreementId
                    this.vgid = vgId
                }
            }
        }.ret.firstOrNull()?.let {
            UserBlockTemplate(
                it.durationmin,
                it.durationmax,
                it.numavailtodestination,
                it.positivebalance == 1L
            )
        }

    fun getUserBlockSchedules(sessionId: String, agreementId: Long, vgId: Long): List<UserBlockSchedule> =
        soapAdapter.withSession(sessionId).request<GetVgUserBlockScheduleResponse> {
            GetVgUserBlockSchedule().apply {
                this.flt = SoapGetVgUserBlockSchedule().apply {
                    this.agrmid = agreementId
                    this.vgid = vgId
                }
            }
        }.ret.map {
            UserBlockSchedule(
                it.recordid,
                it.creationdate,
                it.timefrom,
                it.timeto,
                it.timeto.isDateTimeAfterNow()
            )
        }

    fun setUserBlockSchedule(sessionId: String, vgId: Long, startDate: LocalDate, endDate: LocalDate): Long =
        soapAdapter.withSession(sessionId).request<SetVgUserBlockScheduleResponse> {
            SetVgUserBlockSchedule().apply {
                this.`val` = SoapVgUserBlockSchedule().apply {
                    this.comment = "test"
                    this.vgid = vgId
                    this.timefrom = startDate.toString()
                    this.timeto = endDate.toString()
                    // this.parentid = 0L ???
                }
            }
        }.ret

    fun delVgUserBlockSchedule(sessionId: String, recordId: Long): Long =
        soapAdapter.withSession(sessionId).request<DelVgUserBlockScheduleResponse> {
            DelVgUserBlockSchedule().apply {
                this.id = recordId
            }
        }.ret
}