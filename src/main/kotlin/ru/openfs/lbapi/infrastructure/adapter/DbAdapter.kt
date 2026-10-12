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
            v.vg_id            				  AS vgId,
            v.login            		          AS vLogin,
            v.blocked          				  AS vBlocked,
            v.acc_ondate       				  AS vOnDate,
            t.descr            				  AS tarName,
            t.descr_full       			      AS tarDescr,
            t.type             			      AS tarType,
            t.rent             			      AS tarRent,
            t.rent_as_service  				  AS tarRentAsService,
            t.shape            				  AS tarShape,
            s.state            				  AS sState,
            s.timefrom         				  AS sTimeFrom,
            COALESCE(s.need_calc, 0)          AS sNeedCalc,
            COALESCE(tm.rent, 0)              AS tmRent,
            tm.timefrom                       AS tmTimefrom,
            tm.timeto                         AS tmTimeto,
            COALESCE(tm.service_id, 0)        AS tmServiceId,
            sc.above                          AS scAbove,
            sc.descr                          AS scDescr,
            sc.rent_period                    AS scRentPeriod,
            COALESCE(sc.rent_period_month, 0) AS scRentPeriodMonth,
            COALESCE(sc.dtv_type, 0)          AS scDtvUsecas,
            sc.begin_period                   AS scBeginPeriod,
            va.address                        AS vaAddress
        FROM billing.vgroups v
            JOIN billing.tarifs t ON t.tar_id = v.tar_id
            JOIN billing.vgroups_addr va ON va.vg_id = v.vg_id
            LEFT JOIN billing.services s ON s.vg_id = v.vg_id AND s.need_calc = 1 AND s.state = 3
            LEFT JOIN billing.tariff_modifiers tm ON tm.vg_id = v.vg_id AND tm.timeto > NOW() AND (tm.tar_id = t.tar_id OR tm.service_id = s.service_id)
            LEFT JOIN billing.service_categories sc ON sc.tar_id = v.tar_id AND sc.serv_cat_idx = s.serv_cat_idx
        WHERE v.archive = 0 AND v.agrm_id = ?
        """.trimIndent()
        ).execute(Tuple.of(agreementId))
            .map { rows ->
                rows.groupBy { it.getLong("vgId") }
                    .map { (k, rows) ->
                        val row = rows.first()
                        // resolve rent period
                        val rentPeriod = when {
                            row.getString("tarName").contains("полугодовой") -> "за 6 мес."
                            row.getString("tarName").contains("годовой") -> "в год"
                            else -> MONTHLY
                        }

                        // calc tar rent with discount
                        val dbTarRent = row.getDouble("tarRent")
                        val dbTmRent = row.getDouble("tmRent")
                        val tarRent = when {
                            // calc discount by tarModifiers
                            dbTmRent > 0.0 && dbTmRent < dbTarRent -> dbTmRent
                            else -> dbTarRent
                        }

                        // get tariff services
                        val extService = rows
                            .filter { row.getInteger("sNeedCalc") != 0 || row.getInteger("tmServiceId") != 0 }
                            .mapNotNull { mapService(it) }
                            .filter { it.rent > 0 }

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
                            login = row.getString("vLogin"),
                            address = row.getString("vaAddress"),
                            tarType = mapTarType(row.getInteger("tarType"), row.getInteger("scDtvUsecas")),
                            tarName = row.getString("tarName"),
                            tarShape = convertBpsToReadableFormat(row.getLong("tarShape")),
                            tarRent = tarRent,
                            blocked = row.getInteger("vBlocked") != 0,
                            blockedType = row.getLong("vBlocked"),
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
        return try {
            val rp = row.getInteger("scRentPeriod")
            val rpm = row.getInteger("scRentPeriodMonth")
            val tr = row.getDouble("tmRent")
            val servState = row.getLong("sState")
            ExtService(
                descr = row.getString("scDescr"),
                rent = if (tr > 0) tr else row.getDouble("scAbove"),
                rentPeriod = mapRentPeriod(rp, rpm),
                state = servState,
                stateDescr = if (servState == 3L) "включена" else "отключена",
                nextPayDate = when {
                    rp == 4 -> resolveNextPayDate(row)
                    tr > 0 -> row.getLocalDate("tmTimeto").toString()
                    else -> null
                },
            )
        } catch (e: Exception) {
            Log.warn("Exception while parsing ExtService; ${e.message}")
            null
        }
    }

    private fun resolveNextPayDate(row: Row): String? {
        val rpm = row.getLong("scRentPeriodMonth")
        return when (row.getInteger("scBeginPeriod")) {
            // 0 -> FormatUtil.getDateStartNextMonth()
            // 1 -> "договор"

            // 2 - дата активации УЗ
            2 -> nextPaymentDate(
                startDate = row.getLocalDate("vOnDate"),
                intervalMonths = rpm
            ).toString()

            // 3 -> "дата подключения"
            3 -> nextPaymentDate(
                startDate = row.getLocalDate("sTimeFrom"),
                intervalMonths = rpm
            ).toString()

            else -> null
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
	            tr.record_id, tr.state, tr.request_by, tr.tar_id_new, tr.change_time, 
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
                    state = it.getLong("record_id"),
                    stateDescr = it.getInteger("request_by")?.let { "manager" } ?: "user",
                    nextPayDate = it.getLocalDate("change_time").toString()
                )
            }
        }.await().indefinitely()
    }

}