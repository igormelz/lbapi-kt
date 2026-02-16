package ru.openfs.lbapi.domain.tarif

import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.domain.tarif.model.AvailableTariffDto
import ru.openfs.lbapi.domain.tarif.model.toDto
import ru.openfs.lbapi.infrastructure.adapter.DbAdapter
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import java.time.LocalDate

@ApplicationScoped
class TariffService(
    private val soapAdapter: SoapAdapter,
    private val dbAdapter: DbAdapter
) {

    private val shapeName = mapOf(
        100 to "PRO",
        200 to "PREMIUM",
        300 to "MEGA",
        500 to "MAX",
        700 to "EXTRA"
    )


    fun getTariffsHistory(sessionId: String, agreementId: Long): List<SoapTarifsHistory> {
        return soapAdapter.withSession(sessionId).request<GetTarifsHistoryResponse> {
            GetTarifsHistory().apply {
                flt = SoapFilter().apply {
                    agrmid = agreementId
                }
            }
        }.ret
    }

    fun getTariffsStaff(sessionId: String, tarId: Long): List<SoapTarifsStaff> {
        return soapAdapter.withSession(sessionId).request<GetTarifsStaffResponse> {
            GetTarifsStaff().apply {
                filter = SoapTarifsStaff().apply {
                    grouptarid = tarId
                }
            }
        }.ret
    }

    fun getTariffByServiceId(sessionId: String, vgId: Long, currentRent: Double): List<AvailableTariffDto> {
        return soapAdapter.withSession(sessionId).request<GetClientVgroupsResponse> {
            GetClientVgroups().apply {
                flt = SoapFilter().apply {
                    vgid = vgId
                }
            }
        }.ret.firstOrNull()?.let { vg ->
            val currentTariff = vg.vgroup.tarifid
            val agentId = vg.vgroup.agentid
            val tariffs = dbAdapter.getAvailableTariffs(currentTariff)
            val tariffsByShape = tariffs.groupBy {
                resolveShapeName(it.tarDescr, it.shape )
            }

            tariffs.map { t ->
                val currentShapeName =  resolveShapeName(t.tarDescr, t.shape )
                val shapeTariffs = tariffsByShape[currentShapeName] ?: emptyList()
                val rateMap = shapeTariffs.associateBy { it.rateLevel }
                val yearRate = rateMap[12]?.tarRent ?: currentRent
                val monthRate = rateMap[1]?.tarRent ?: currentRent

                t.toDto(
                    tarName = currentShapeName,
                    currentTarId = currentTariff,
                    agentId = agentId,
                    discount = when (t.rateLevel) {
                        1 -> yearRate.let { t.tarRent * 12 - it }
                        6 -> monthRate.let { it * 6 - t.tarRent }
                        else -> monthRate.let { it * 12 - t.tarRent }
                    }
                )
            }
        } ?: emptyList()
    }

    private fun resolveShapeName(description: String, shape: Int): String =
        when {
            description.endsWith("LITE-F") -> "LITE" // 50
            description.endsWith("ULTRA-F") -> "ULTRA" // 700
            description.endsWith("SMART-F") -> "SMART" // 100
            description.endsWith("DRIVE-F") -> "DRIVE" // 200
            // default mapping
            else -> shapeName.getOrDefault(shape, "PRO")
        }


    /*
            return $this->s('insClientTarifsRasp', array(
            'recordid'   => 0,
            'vgid'       => $this->param('vgid'),
            'id'         => $this->vgroup()->vgroup->agentid,
            'taridnew'   => $this->param('tarid'),
            'taridold'   => $this->vgroup()->vgroup->tarifid,
            'changetime' => $this->getChangeTime(),
            'requestby'  => '',
            'servcatidx' => $this->param('servcatidx')
     */
    fun addTariffSchedule(
        sessionId: String,
        vgId: Long,
        agentId: Long,
        tarIdOld: Long,
        tarIdNew: Long,
        changeDate: LocalDate,
        serviceCat: Long?
    ): Long {
        Log.info("add tariff schedule for [$vgId] from [$tarIdOld] to [$tarIdNew] on [$changeDate] with session:[$sessionId]")
        return soapAdapter.withSession(sessionId).request<InsClientTarifsRaspResponse> {
            InsClientTarifsRasp().apply {
                `val` = SoapTarifsRasp().apply {
                    recordid = 0L
                    vgid = vgId
                    id = agentId
                    taridnew = tarIdNew
                    taridold = tarIdOld
                    changetime = changeDate.toString()
                    servcatidx = serviceCat
                }
            }
        }.ret
    }

    fun deleteTariffSchedule(
        sessionId: String,
        recordId: Long
    ): Long {
        Log.info("remove tariff schedule for [$recordId] with session:[$sessionId]")
        return soapAdapter.withSession(sessionId).request<DelClientTarifsRaspResponse> {
            DelClientTarifsRasp().apply {
                id = recordId
            }
        }.ret
    }

}