package ru.openfs.lbapi.infrastructure.adapter

import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.Tuple
import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.domain.agreement.model.ClientVGroup
import ru.openfs.lbapi.domain.agreement.model.ExtService
import ru.openfs.lbapi.domain.agreement.model.RentByPeriod
import ru.openfs.lbapi.domain.agreement.model.ServiceInfo
import ru.openfs.lbapi.common.utils.FormatUtil.convertBpsToReadableFormat

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
            	s.need_calc,
            	s.state,
            	s.serv_cat_idx,
            	sc.above,
            	sc.descr,
            	sc.descr_full,
            	sc.rent_period,
            	sc.rent_period_month,
            	sc.service_type,
            	COALESCE(sc.dtv_type, 0) as usecas,
            	va.address
            from billing.vgroups v 
                inner join billing.tarifs t using (tar_id) 
                inner join billing.vgroups_addr va using (vg_id)
                left join (select * from billing.services where need_calc = 1) s USING (vg_id)
                left join billing.service_categories sc on (sc.serv_cat_idx = s.serv_cat_idx and v.tar_id = sc.tar_id)
            where agrm_id = ?
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
                        val extService = rows.mapNotNull { ExtService.fromRow(it) }.filter { it.rent > 0 }

                        val rentSummary = (listOf(rentPeriod to tarRent) + extService.map { it.rentPeriod to it.rent })
                            .groupBy({ it.first }, { it.second })
                            .map { (period, rent) ->
                                RentByPeriod(
                                    rentPeriod = period,
                                    rentAmount = rent.sum()
                                )
                            }

                        ServiceInfo(
                            id = k,
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
                            rentSummary = rentSummary
                        )
                    }
                    // keep only valid services
                    .filterNot { it.tarType == "services" && it.tarRent == 0.0 && it.extService.isEmpty() }
            }.await().indefinitely().firstOrNull()
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