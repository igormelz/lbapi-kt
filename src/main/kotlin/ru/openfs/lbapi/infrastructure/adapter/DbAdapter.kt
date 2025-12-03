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
import ru.openfs.lbapi.domain.tarif.model.AvailableTariff

@ApplicationScoped
class DbAdapter(
    private val client: Pool
) {
    private val MONTHLY = "в мес."
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
	s.timefrom
FROM
	billing.vgroups v
INNER JOIN billing.tarifs t ON
	v.tar_id = t.tar_id
INNER JOIN billing.vgroups_addr va ON
	v.vg_id = va.vg_id
LEFT JOIN billing.services s ON
	v.vg_id = s.vg_id
	AND s.need_calc = 1
	AND s.state = 3
left join billing.tariff_modifiers tm on
	tm.tar_id = t.tar_id
	and tm.vg_id = v.vg_id
	and tm.service_id = s.service_id
LEFT JOIN billing.service_categories sc ON
	sc.serv_cat_idx = s.serv_cat_idx
	AND v.tar_id = sc.tar_id
WHERE
	v.archive = 0
	AND v.agrm_id = ?
""".trimIndent()
        ).execute(Tuple.of(agreementId))
            .map { rows ->
                rows.groupBy { it.getLong("vg_id") }
                    .map { (k, rows) ->
                        val row = rows.first()
                        val rentPeriod = when {
                            row.getString("tarName").contains("полугодовой") -> "за 6 мес."
                            row.getString("tarName").contains("годовой") -> "в год"
                            else -> MONTHLY
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
                            changeTo = null,
                        )
                    }
                    // keep only valid services
                    .filterNot { it.tarType == "services" && it.tarRent == 0.0 && it.extService.isEmpty() }
            }.await().indefinitely().firstOrNull()
    }

    private fun mapRentPeriod(rp: Int, rpm: Int): String =
        when (rp) {
            0 -> "разовое"
            1 -> MONTHLY
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

    fun getAvailableTariffs(tarId: Long): List<AvailableTariff> {
        return client.preparedQuery(
            """
            SELECT 
                t.tar_id, t.descr, t.descr_full, t.rent, t.rent_as_service, t.shape, 
                sc.rent_period_month, sc.above, sc.serv_cat_idx 
            FROM (SELECT tar_id FROM tarifs_staff WHERE group_tar_id = ?) ts
            JOIN tarifs t ON t.tar_id = ts.tar_id
            LEFT JOIN service_categories sc ON sc.tar_id = t.tar_id AND sc.service_type = 3 
        """.trimIndent()
        ).execute(Tuple.of(tarId))
            .map { rows ->
                rows.map {
                    val shape = it.getInteger("shape") / 1024

                    when (it.getInteger("rent_as_service")) {
                        0 -> AvailableTariff(
                            tarId = it.getLong("tar_id"),
                            tarDescr = it.getString("descr"),
                            tarRent = it.getDouble("rent"),
                            rateLevel = 1,
                            shape = shape,
                        )

                        else -> AvailableTariff(
                            tarId = it.getLong("tar_id"),
                            tarDescr = it.getString("descr"),
                            tarRent = it.getDouble("above"),
                            rateLevel = it.getInteger("rent_period_month"),
                            shape = shape,
                            serviceCat = it.getLong("serv_cat_idx"),
                        )
                    }
                }
            }.await().indefinitely()
    }

    fun getScheduledChangeTariffs(vgId: Long): List<ExtService> {
        return client.preparedQuery(
            """
            SELECT 
	            tr.state, tr.request_by, tr.tar_id_new, tr.change_time, 
                t.rent, t.rent_as_service, t.descr,
                sc.above, sc.rent_period, sc.rent_period_month
            FROM
	            tarifs_rasp tr INNER JOIN tarifs t ON t.tar_id = tr.tar_id_new
                LEFT JOIN service_categories sc ON sc.serv_cat_idx = tr.serv_cat_idx AND sc.tar_id = tr.tar_id_new
            WHERE tr.vg_id = ?
        """.trimIndent()
        ).execute(Tuple.of(vgId)).map { rows ->
            rows.map {
                ExtService(
                    descr = it.getString("descr"),
                    rent = it.getDouble("above") ?: it.getDouble("rent"),
                    rentPeriod = mapRentPeriod(it.getInteger("rent_period") ?: 1, it.getInteger("rent_period_month") ?: 1),
                    state = it.getLong("state"),
                    stateDescr = it.getInteger("request_by")?.let { "manager" } ?: "user",
                    nextPayDate = it.getLocalDate("change_time").toString()
                )
            }
        }.await().indefinitely()
    }

}