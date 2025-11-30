package ru.openfs.lbapi.domain.tarif

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.domain.tarif.model.AvailableTariff
import ru.openfs.lbapi.infrastructure.adapter.DbAdapter
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter

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

    fun getTariffByServiceId(sessionId: String, vgId: Long, currentRent: Double): List<AvailableTariff> {
        return soapAdapter.withSession(sessionId).request<GetClientVgroupsResponse> {
            GetClientVgroups().apply {
                flt = SoapFilter().apply {
                    vgid = vgId
                }
            }
        }.ret.firstOrNull()?.let {
            val tariffs = dbAdapter.getAvailableTariffs(it.vgroup.tarifid)
            val tariffsByShape = tariffs.groupBy {
                shapeName.getOrDefault(it.shape, "PRO")
            }

            tariffs.map { t ->
                val currentShapeName = shapeName.getOrDefault(t.shape, "PRO")
                val shapeTariffs = tariffsByShape[currentShapeName] ?: emptyList()
                val rateMap= shapeTariffs.associateBy { it.rateLevel }
                val yearRate = rateMap[12]?.tarRent
                val monthRate = rateMap[1]?.tarRent ?: currentRent
                when (t.rateLevel) {
                    1 -> t.copy(tarName = currentShapeName, discount = yearRate?.let { t.tarRent * 12 - it })
                    6 -> t.copy(tarName = currentShapeName, discount = yearRate?.let { t.tarRent * 2 - it })
                    else -> t.copy(tarName = currentShapeName, discount = monthRate.let { it * 12 - t.tarRent })
                }
            }
        } ?: emptyList()
    }


}