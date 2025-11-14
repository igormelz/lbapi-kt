package ru.openfs.lbapi.infrastructure.adapter

import io.quarkus.logging.Log
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.Row
import io.vertx.mutiny.sqlclient.Tuple
import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.common.utils.FormatUtil.convertBpsToReadableFormat
import ru.openfs.lbapi.common.utils.FormatUtil.nextPaymentDate
import ru.openfs.lbapi.domain.agreement.model.ChangeTariff
import ru.openfs.lbapi.domain.agreement.model.ClientVGroup
import ru.openfs.lbapi.domain.agreement.model.ExtService
import ru.openfs.lbapi.domain.agreement.model.RentByPeriod
import ru.openfs.lbapi.domain.agreement.model.ServiceInfo

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
                        select
                        	v.vg_id,
                        	v.login,
                        	v.blocked,
                        	v.amount,
                        	v.acc_ondate,
                        	t.descr as tarName,
                        	t.descr_full as tarDescr,
                        	t.`type` as tarType,
                        	t.rent as tarRent,
                        	t.rent_as_service,
                        	t.additional,
                        	t.shape as tarShape,
                        	COALESCE(s.need_calc, 0) as need_calc,
                        	s.state,
                        	s.serv_cat_idx,
                        	sc.above,
                        	sc.descr,
                        	sc.descr_full,
                        	sc.rent_period,
                        	sc.rent_period_month,
                        	sc.service_type, 
                        	COALESCE(sc.dtv_type, 0) as usecas,
                        	va.address,
                            sc.begin_period,
                            s.activated,
                            s.timefrom,
                            tr.tar_new_id as trf_tar_id,
                            tr.change_time as trf_change_date_time,
                            trf.rent as trf_rent,
                            trf.descr as trf_descr,
                            tsc.above as trf_above,
                            tsc.rent_period as trf_rent_period,
                            tsc.rent_period_month as trf_rent_period_month
                        from billing.vgroups v 
                            inner join billing.tarifs t using (tar_id) 
                            inner join billing.vgroups_addr va using (vg_id)
                            left join (select * from billing.services where need_calc = 1) s USING (vg_id)
                            left join billing.service_categories sc on (sc.serv_cat_idx = s.serv_cat_idx and v.tar_id = sc.tar_id)
                            left join billing.tarifs_rasp tr using (vg_id)
                            left join billing.tarifs trf on (trf.tar_id = tr.tar_id_new)
                            left join billing.service_categories tsc on (tsc.serv_cat_idx = tr.serv_cat_idx and tsc.tar_id = tr.tar_id_new)
            where v.archive = 0 and agrm_id = ?
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
                            changeTariff = changeTariff
                        )
                    }
                    // keep only valid services
                    .filterNot { it.tarType == "services" && it.tarRent == 0.0 && it.extService.isEmpty() }
            }.await().indefinitely().firstOrNull()
    }

    fun mapChangeTariff(row: Row): ChangeTariff? {
        return row.getLong("trf_tar_id")?.let {
            ChangeTariff(
                tarId = it,
                tarName = row.getString("trf_descr"),
                rent = row.getDouble("trf_rent"),
                changeDateTime = row.getString("trf_change_date_time"),
            )
        }
    }
    fun mapService(row: Row): ExtService? {
        return try {
            if (row.getInteger("need_calc") == 0 || row.getInteger("state") != 3) {
                null
            } else {
                val rp = row.getInteger("rent_period")
                val rpm = row.getInteger("rent_period_month")
                ExtService(
                    descr = row.getString("descr"),
                    rent = row.getDouble("above"),
                    rentPeriod = when (rp) {
                        0 -> "разовое"
                        1 -> "в мес."
                        2 -> "в день"
                        3 -> "ежедневное равными долями"
                        4 -> if (rpm == 12) "в год" else "за 6 мес."
                        else -> "NaN"
                    },
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

                            else -> ""
                        }
                    } else ""
                )
            }
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