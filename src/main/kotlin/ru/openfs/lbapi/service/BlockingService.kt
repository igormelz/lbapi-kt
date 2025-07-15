package ru.openfs.lbapi.service

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.model.UserBlockSchedule
import ru.openfs.lbapi.model.UserBlockTemplate
import ru.openfs.lbapi.utils.FormatUtil.isDateTimeAfterNow
import java.time.LocalDate

@ApplicationScoped
class BlockingService(
    private val clientService: SoapClientService,
) {

    fun getUserBlockTemplate(sessionId: String, agreementId: Long, vgId: Long): UserBlockTemplate? =
        clientService.withSession(sessionId).request<GetUserBlockTemplateResponse> {
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

    fun getVgUserBlockSchedule(sessionId: String, agreementId: Long, vgId: Long): List<UserBlockSchedule> =
        clientService.withSession(sessionId).request<GetVgUserBlockScheduleResponse> {
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

    fun setVgUserBlockSchedule(sessionId: String, vgId: Long, startDate: LocalDate, endDate: LocalDate): Long =
        clientService.withSession(sessionId).request<SetVgUserBlockScheduleResponse> {
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
        clientService.withSession(sessionId).request<DelVgUserBlockScheduleResponse> {
            DelVgUserBlockSchedule().apply {
                this.id = recordId
            }
        }.ret
}