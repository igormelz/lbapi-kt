package ru.openfs.lbapi.infrastructure.adapter

import io.quarkus.logging.Log
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.Row
import io.vertx.mutiny.sqlclient.Tuple
import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.common.utils.FormatUtil.convertBpsToReadableFormat
import ru.openfs.lbapi.common.utils.FormatUtil.nextPaymentDate
import ru.openfs.lbapi.domain.agreement.model.ClientVGroup
import ru.openfs.lbapi.domain.agreement.model.ExtService
import ru.openfs.lbapi.domain.agreement.model.RentByPeriod
import ru.openfs.lbapi.domain.agreement.model.ServiceInfo
import java.time.LocalDate

@ApplicationScoped
class DbAdapter(
    private val client: Pool
) {

    fun getVGroups(agreementId: Long): List<ClientVGroup> {
        return client.preparedQuery(
            """
             select v.vg_id, v.login, t.tar_id, t.descr 
             from billing.vgroups v inner join billing.tarifs t using (tar_id)
             where agrm_id = ?
            """.trimIndent()
        ).execute(Tuple.of(agreementId))
            .map { rows ->
                rows.map {
                    ClientVGroup(
                        it.getLong("vg_id"),
                        it.getString("login"),
                        it.getLong("tar_id"),
                        it.getString("descr"),
                    )
                }
            }.await().indefinitely()
    }

    fun getVGroupsAndServices(agreementId: Long): ServiceInfo? {
        return client.preparedQuery(
            """
SELECT
    v.vg_id,
    v.login,
    v.blocked,
    v.amount,
    v.acc_ondate,
    t.descr AS tarName,
    t.descr_full AS tarDescr,
    t.type AS tarType,
    t.rent AS tarRent,
    t.rent_as_service,
    t.additional,
    t.shape AS tarShape,
    COALESCE(s.need_calc, 0) AS need_calc,
    COALESCE(tm.rent, 0) tm_rent,
    tm.timeto tm_timeto,
    s.state,
    s.serv_cat_idx,
    sc.above,
    sc.descr,
    sc.descr_full,
    sc.rent_period,
    COALESCE(sc.rent_period_month, 0) as rent_period_month,
    sc.service_type, 
    COALESCE(sc.dtv_type, 0) AS usecas,
    va.address,
    sc.begin_period,
    s.activated,
    s.timefrom,
    tr.tar_id_new AS trf_tar_id,
    tr.change_time AS trf_change_date_time,
    trf.rent AS trf_rent,
    trf.descr AS trf_descr,
    tsc.above AS trf_above,
    tsc.rent_period AS trf_rent_period,
    tsc.rent_period_month AS trf_rent_period_month
FROM billing.vgroups v 
INNER JOIN billing.tarifs t ON v.tar_id = t.tar_id
INNER JOIN billing.vgroups_addr va ON v.vg_id = va.vg_id
LEFT JOIN billing.services s ON v.vg_id = s.vg_id AND s.need_calc = 1 AND s.state = 3
left join billing.tariff_modifiers tm on tm.tar_id = t.tar_id and tm.vg_id = v.vg_id and tm.service_id = s.service_id 
LEFT JOIN billing.service_categories sc ON sc.serv_cat_idx = s.serv_cat_idx AND v.tar_id = sc.tar_id
LEFT JOIN billing.tarifs_rasp tr ON v.vg_id = tr.vg_id
LEFT JOIN billing.tarifs trf ON trf.tar_id = tr.tar_id_new
LEFT JOIN billing.service_categories tsc ON tsc.serv_cat_idx = tr.serv_cat_idx AND tsc.tar_id = tr.tar_id_new
WHERE v.archive = 0 AND v.agrm_id = ?
""".trimIndent()
        ).execute(Tuple.of(agreementId))
            .map { rows ->
                rows.groupBy { it.getLong("vg_id") }
                    .map { (k, rows) ->
                        val row = rows.first()
                        val rentPeriod = when {
                            row.getString("tarName").contains("полугодовой") -> "за 6 мес."
                            row.getString("tarName").contains("годовой") -> "в год"
                            else -> "в мес."
                        }
                        val tarRent = row.getDouble("tarRent")
                        val extService = rows.mapNotNull { mapService(it) }.filter { it.rent > 0 }

                        val rentSummary = (listOf(rentPeriod to tarRent) + extService.map { it.rentPeriod to it.rent })
                            .filter { it.second > 0 }
                            .groupBy({ it.first }, { it.second })
                            .map { (period, rent) ->
                                RentByPeriod(
                                    rentPeriod = period,
                                    rentAmount = rent.sum()
                                )
                            }
                        val changeTariff = rows.map { mapChangeTariff(row) }.firstOrNull()
                        ServiceInfo(
                            id = k, // vg_id
                            login = row.getString("login"),
                            address = row.getString("address"),
                            tarType = mapTarType(row.getInteger("tarType"), row.getInteger("usecas")),
                            tarName = row.getString("tarName"),
                            tarShape = convertBpsToReadableFormat(row.getLong("tarShape")),
                            tarRent = tarRent,
                            blocked = row.getInteger("blocked") != 0,
                            blockedType = row.getLong("blocked"),
                            rentPeriod = rentPeriod,
                            extService = extService,
                            rentSummary = rentSummary,
                            changeTo = changeTariff
                        )
                    }
                    // keep only valid services
                    .filterNot { it.tarType == "services" && it.tarRent == 0.0 && it.extService.isEmpty() }
            }.await().indefinitely().firstOrNull()
    }

    fun mapChangeTariff(row: Row): ExtService? {
        return row.getLong("trf_tar_id")?.let {
            ExtService(
                descr = row.getString("trf_descr"),
                rent = row.getDouble("trf_above"),
                rentPeriod = mapRentPeriod(row.getInteger("trf_rent_period"), row.getInteger("trf_rent_period_month")),
                nextPayDate = row.getLocalDate("trf_change_date_time").toString(),
                state = 0,
                stateDescr = "change"
            )
        }
    }

    private fun mapRentPeriod(rp: Int, rpm: Int): String =
        when (rp) {
            0 -> "разовое"
            1 -> "в мес."
            2 -> "в день"
            3 -> "ежедневное равными долями"
            4 -> if (rpm == 12) "в год" else "за 6 мес."
            else -> "NaN"
        }

    fun mapService(row: Row): ExtService? {
        if (row.getInteger("need_calc") == 0) return null
        return try {
            val rp = row.getInteger("rent_period")
            val rpm = row.getInteger("rent_period_month")
            val tr = row.getDouble("tm_rent")
            ExtService(
                descr = row.getString("descr"),
                rent = if (tr > 0) tr else row.getDouble("above"),
                rentPeriod = mapRentPeriod(rp, rpm),
                state = row.getLong("state"),
                stateDescr = when (row.getInteger("state")) {
                    0, 2 -> "отключена"
                    else -> "включена"
                },
                nextPayDate = if (rp == 4) {
                    when (row.getInteger("begin_period")) {
                        // 0 -> FormatUtil.getDateStartNextMonth()
                        // 1 -> "договор"
                        2 -> nextPaymentDate(
                            startDate = row.getLocalDate("acc_ondate"),
                            intervalMonths = rpm.toLong()
                        ).toString()
                        // 3 -> "подключение"
                        3 -> nextPaymentDate(
                            startDate = row.getLocalDate("timefrom"),
                            intervalMonths = rpm.toLong()
                        ).toString()

                        else -> null
                    }
                } else if (tr > 0) row.getLocalDate("tm_timeto").toString() else null
            )
        } catch (e: Exception) {
            Log.warn("Exception while parsing ExtService; ${e.message}")
            null
        }
    }


    fun setAccountNotice(uid: Long, type: Long, transport: Long, value: String): Boolean {
        return client.preparedQuery(
            """
            INSERT INTO account_notices (uid, notice_type, transport, more) VALUES (?, ?, ?, ?) 
            ON DUPLICATE KEY UPDATE transport=VALUES(transport), more=VALUES(more)
            """.trimIndent()
        ).execute(Tuple.of(uid, type, transport, value))
            .map { it.rowCount() == 1 }
            .await().indefinitely()
    }

    fun delAccountNotice(uid: Long, type: Long): Boolean {
        return client.preparedQuery("DELETE FROM account_notices WHERE uid=? and notice_type=?")
            .execute(Tuple.of(uid, type))
            .map { it.rowCount() == 1 }
            .await().indefinitely()
    }

    private fun mapTarType(tarType: Int, usecas: Int): String =
        when {
            tarType < 3 -> "internet"
            tarType == 5 && usecas == 1 -> "DTV"
            tarType == 5 -> "services"
            else -> "Phone"
        }


}