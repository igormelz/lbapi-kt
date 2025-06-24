package ru.openfs.lbapi.service

import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.exception.ApiException
import ru.openfs.lbapi.model.*
import ru.openfs.lbapi.service.adapter.DbAdapter
import ru.openfs.lbapi.service.adapter.EmailAdapter
import ru.openfs.lbapi.service.adapter.LbCoreSoapAdapter
import ru.openfs.lbapi.utils.FormatUtil.isDateTimeAfterNow
import java.time.LocalDate
import java.time.Month

@ApplicationScoped
class LbApiService(
    private val adapter: LbCoreSoapAdapter,
    private val dbAdapter: DbAdapter,
    private val emailAdapter: EmailAdapter,
) {

    fun isApiReady(): Boolean {
        try {
            return adapter.getResponseAsMandatoryType(
                null,
                GetVersion(),
                TVersion::class.java
            ).second.version.isNotEmpty()
        } catch (_: ApiException) {
            return false
        }
    }

    fun startSession(login: String, password: String) =
        adapter.getSessionId(
            ClientLogin().apply {
                this.login = login
                this.pass = password
            }
        )

    fun closeSession(sessionId: String) = adapter.getResponseAsString(sessionId, Logout())

    fun getAccounts(sessionId: String): SoapAccountFull =
        adapter.getResponseAsMandatoryType(
            sessionId,
            GetClientAccount().apply {
                this.flt = SoapGetAccountFilter().apply {
                    this.activonly = 1L
                }
            },
            GetClientAccountResponse::class.java
        ).second.ret.first()

    fun getAccountIsEmailConfirm(login: String): Boolean =
        adapter.getResponseAsMandatoryType(
            null,
            GetInfoAboutAccountDataConfirm().apply {
                this.accountlogin = login
            },
            SoapInfoAboutAccountDataConfirmResponse::class.java
        ).second.isEmailisconfirmed

    fun getRecoveryPassword(login: String): SoapRecoverPassword =
        adapter.getResponseAsMandatoryType(
            null,
            RecoverPassword().apply {
                this.login = login
                this.transport = 0 // email
                this.isSimplerecover = false
                this.isSync = false
            },
            RecoverPasswordResponse::class.java
        ).second.ret.first()

    fun updatePasswordByCode(login: String, password: String, code: String): String? =
        adapter.getResponseAsMandatoryType(
            null,
            UpdatePasswordByCode().apply {
                this.`val` = SoapUpdatePasswordByCode().apply {
                    this.login = login
                    this.password = password
                    this.acceptcode = code
                }
            },
            UpdatePasswordByCodeResponse::class.java
        ).second.ret

    fun updatePasswordByToken(password: String, token: String): String? =
        adapter.getResponseAsMandatoryType(
            null,
            UpdatePasswordByToken().apply {
                this.`val` = SoapUpdatePasswordByToken().apply {
                    this.password = password
                    this.token = token
                }
            },
            UpdatePasswordByTokenResponse::class.java
        ).second.ret

    private fun getOptionByName(sessionId: String, optionName: String? = "pass_length"): SoapOption? =
        adapter.getResponseAsMandatoryType(
            sessionId,
            GetOptionByName().apply {
                this.name = optionName
            },
            GetOptionByNameResponse::class.java
        ).second.ret.firstOrNull()

    fun getPassTemplate(sessionId: String): PasswordTemplate {
        if (getOptionByName(sessionId)?.availabilitytype?.toInt() != 0) throw ApiException("not allowed")

        return adapter.getResponseAsMandatoryType(
            sessionId,
            GetPassTemplates().apply {
                this.flt = SoapFilter().apply {
                    this.objecttype = "account"
                }
            },
            GetPassTemplatesResponse::class.java
        ).second.ret
            .firstOrNull()
            .let {
                PasswordTemplate(
                    minLength = it?.minlength ?: 0,
                    hasSpecialChars = it?.needspecial == 1L,
                    hasUppercase = it?.needupper == 1L,
                    hasDigits = it?.neednumbers == 1L,
                    hasLowercase = it?.needlow == 1L,
                    checkPassword = it?.checkpass == 1L,
                    excludeChars = it?.excluded,
                )
            }
    }

    fun updatePassword(sessionId: String, oldPass: String, newPass: String): Boolean =
        adapter.getResponseAsMandatoryType(
            sessionId,
            UpdClientPass().apply {
                this.oldpass = oldPass
                this.newpass = newPass
            },
            UpdClientPassResponse::class.java
        ).second.ret == 1L

    fun getAccountNotices(sessionId: String): List<SoapAccountNotices?> =
        adapter.getResponseAsMandatoryType(
            sessionId,
            GetAccountNotices(),
            GetAccountNoticesResponse::class.java
        ).second.ret

    fun getSharedPostsCategories(sessionId: String): List<SoapSharedPostsCategory?> =
        adapter.getResponseAsMandatoryType(
            sessionId,
            GetSharedPostsCategories(),
            GetSharedPostsCategoriesResponse::class.java
        ).second.ret

    fun getAccountSharedPostsCategories(sessionId: String): List<SoapAccountSharedPostCategories?> =
        adapter.getResponseAsMandatoryType(
            sessionId,
            GetAccountSharedPostCategories(),
            GetAccountSharedPostCategoriesResponse::class.java
        ).second.ret

    fun getSharedPosts(sessionId: String): List<SoapSharedPost?> =
        adapter.getResponseAsMandatoryType(
            sessionId,
            GetClientSharedPosts().apply {
                this.flt = SoapFilter().apply {
                    //this.pgnum = 1
                    //this.pgsize = 1
                    this.status = "1"
                }
                this.ord.add(
                    SoapOrderby().apply {
                        this.name = "is_auto"
                        this.ascdesc = 1L
                    })
                this.ord.add(
                    SoapOrderby().apply {
                        this.name = "posttime"
                        this.ascdesc = 1L
                    }
                )
            },
            GetClientSharedPostsResponse::class.java
        ).second.ret

    fun getClientInfo(sessionId: String): SoapGetClientInfo =
        adapter.getResponseAsMandatoryType(
            sessionId,
            GetClientInfo(),
            GetClientInfoResponse::class.java
        ).second.ret.first()

    fun getAgreementStat(sessionId: String, agreementId: Long) =
        dbAdapter.getVGroups(agreementId).flatMap { vGroup ->
            adapter.getClientTarStat(sessionId, agreementId, vGroup.vgId)
                .data.map { v ->
                    InvoiceData(
                        // vg names: [uid, agrm_id, vg_id, period, tar_id, rent, serv, above, pays]
                        vgId = v.`val`[2].toLong(),
                        tarId = v.`val`[4].toLong(),
                        serviceName = vGroup.descr,
                        //period = v.`val`[3],
                        charges = v.`val`[5].toDouble() + v.`val`[6].toDouble() + v.`val`[7].toDouble(),
                    )
                } +
                    adapter.getClientSrvStat(sessionId, agreementId, vGroup.vgId)
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

//    private fun calcDiscount(amount: Double, rate: Double, discount: Double): Double {
//        return when {
//            rate != 1.0 -> amount * rate
//            discount == 0.0 -> amount
//            else -> amount - discount
//        }
//    }

//    private fun getServiceInfo(sessionId: String, agreementId: Long) =
//        adapter
//            .getClientVGroupByAgreement(sessionId, agreementId)
//            // keep inet and onetime
//            //.filter { it.vgroup.tariftype == 1L || it.vgroup.tariftype == 5L }
//            .map {
//                ServiceInfo(
//                    id = it.vgroup.vgid,
//                    login = it.vgroup.login,
//                    address = it.addresses.first().address,
//                    tarName = it.vgroup.tarifdescr,
//                    tarShape = convertBpsToReadableFormat(it.vgroup.curshape),
//                    //tarRent = it.vgroup.servicerent,
//                    tarRent = calcDiscount(
//                        it.vgroup.servicerent,
//                        if (it.vgroup.currentmodifier.type == "rate") it.vgroup.currentmodifier.value else 1.0,
//                        if (it.vgroup.currentmodifier.type == "discount") it.vgroup.currentmodifier.value else 0.0,
//                    ),
//                    blocked = it.vgroup.blocked != 0L,
//                    blockedType = it.vgroup.blocked,
//                    tarType = when {
//                        it.vgroup.tariftype < 3L -> "internet"
//                        it.vgroup.tariftype == 5L -> "services"
//                        it.vgroup.tariftype == 5L && it.vgroup.usecas == 1L -> "DTV"
//                        it.vgroup.tariftype == 3L || it.vgroup.tariftype == 4L -> "Phone"
//                        else -> "NaN"
//                    },
//                    rentPeriod = when {
//                        it.vgroup.tarifdescr.contains("полугодовой") -> "за 6 мес."
//                        it.vgroup.tarifdescr.contains("годовой") -> "за 12 мес."
//                        else -> "в мес."
//                    },
//                    extService = adapter.getUsboxService(sessionId, it.vgroup.vgid)?.let {
//                        ExtService(
//                            descr = it.catdescr,
//                            rent = calcDiscount(
//                                it.catabove,
//                                if (it.currentmodifier.type == "rate") it.currentmodifier.value else 1.0,
//                                if (it.currentmodifier.type == "discount") it.currentmodifier.value else 0.0,
//                            ),
//                            rentPeriod = when(it.common) {
//                                0L -> "разовое"
//                                1L -> "в мес."
//                                2L -> "в день"
//                                3L -> "ежедневное равными долями"
//                                4L -> "за 12 мес."
//                                else -> "NaN"
//                            },
//                            it.service.state,
//                            when(it.service.state) {
//                                0L, 2L -> "отключена"
//                                else -> "включена"
//                            }
//                        )
//                    }
//                )
//            }


    // get agreements info
    fun getAgreementsInfo(sessionId: String): List<AgreementInfo> =
        getAccounts(sessionId).agreements.map { agreement ->
            val serviceInfo = dbAdapter.getVGroupsAndServices(agreement.agrmid).firstOrNull()
            AgreementInfo(
                agreement.agrmid,
                agreement.number,
                agreement.date,
                agreement.balance,
                adapter.getRecommendedPayment(sessionId, agreement.agrmid, 3L),
                adapter.getRecommendedPayment(sessionId, agreement.agrmid, 0L),
                agreement.promisecredit,
                agreement.paymentmethod == 1L,
                agreement.credit,
                serviceInfo,
                if (agreement.promisecredit > 0.0) {
                    PromiseCredit(
                        agreement.promisecredit,
                        getClientPromisePayments(
                            sessionId,
                            agreement.agrmid
                        ).firstOrNull { it.status == 3.toShort() }?.promtill
                    )
                } else null,
                if (serviceInfo != null) {
                    getVgUserBlockSchedule(sessionId, agreement.agrmid, serviceInfo.id)
                        .firstOrNull { it.isActive }
                } else null
            )
        }

    fun getClientPromiseSettings(sessionId: String, agreementId: Long): PromiseSettings {
        return adapter.getResponseAsMandatoryType(
            sessionId,
            GetClientPPSettings().apply { this.agrm = agreementId },
            GetClientPPSettingsResponse::class.java
        ).second.ret.first().let {
            PromiseSettings(
                isAllowed = it.promiseavailable == 1L,
                dateLimit = LocalDate.now().plusDays(it.promisetill),
                maxAmount = it.promisemax,
                minAmount = it.promisemin,
                limitAmount = it.promiselimit
            )
        }
    }

    fun getClientPromisePayments(
        sessionId: String,
        agreementId: Long
    ): List<SoapPromisePayment> {
        return adapter.getResponseAsMandatoryType(
            sessionId,
            GetClientPromisePayments().apply {
                this.flt = SoapFilter().apply {
                    this.agrmid = agreementId
                    this.dtfrom = LocalDate.now().minusMonths(1L).toString()
                    this.dtto = LocalDate.now().plusMonths(1L).toString()
                }
            },
            GetClientPromisePaymentsResponse::class.java
        ).second.ret
    }

    fun getClientPayments(
        sessionId: String,
        agreementId: Long,
        year: Int? = null,
    ): List<PaymentsInfo> {
        return adapter.getResponseAsMandatoryType(
            sessionId,
            GetClientPayments().apply {
                this.flt = SoapFilter().apply {
                    this.agrmid = agreementId
                    this.dtfrom = if (year == null)
                        LocalDate.of(LocalDate.now().year, Month.JANUARY, 1).toString()
                    else
                        LocalDate.of(year, Month.JANUARY, 1).toString()
                    this.dtto = if (year == null)
                        LocalDate.now().plusDays(1L).toString()
                    else
                        LocalDate.of(year, Month.DECEMBER, 31).plusDays(1L).toString()
                }
            },
            GetClientPaymentsResponse::class.java
        ).second.ret.map {
            PaymentsInfo(
                paymentType = it.mgr,
                paymentCode = it.pay.comment,
                paymentId = it.pay.receipt,
                paymentAmount = it.amountcurr,
                paymentDate = it.pay.paydate
            )
        }
    }

    fun sentEmailConfirmed(): Uni<String> {
        return emailAdapter.sendEmail("TEST")
    }

    fun getUserBlockTemplate(
        sessionId: String,
        agreementId: Long,
        vgId: Long,
    ): UserBlockTemplate? = adapter.getResponseAsMandatoryType(
        sessionId,
        GetUserBlockTemplate().apply {
            this.flt = SoapGetUserBlockTemplate().apply {
                this.agrmid = agreementId
                this.vgid = vgId
            }
        },
        GetUserBlockTemplateResponse::class.java
    ).second.ret.firstOrNull()?.let {
        UserBlockTemplate(
            it.durationmin,
            it.durationmax,
            it.numavailtodestination,
            it.positivebalance == 1L
        )
    }

    fun getVgUserBlockSchedule(
        sessionId: String,
        agreementId: Long,
        vgId: Long,
    ): List<UserBlockSchedule> = adapter.getResponseAsMandatoryType(
        sessionId,
        GetVgUserBlockSchedule().apply {
            this.flt = SoapGetVgUserBlockSchedule().apply {
                this.agrmid = agreementId
                this.vgid = vgId
            }
        },
        GetVgUserBlockScheduleResponse::class.java
    ).second.ret.map {
        UserBlockSchedule(
            it.recordid,
            it.creationdate,
            it.timefrom,
            it.timeto,
            it.timeto.isDateTimeAfterNow()
        )
    }

    fun setVgUserBlockSchedule(
        sessionId: String,
        vgId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long = adapter.getResponseAsMandatoryType(
        sessionId,
        SetVgUserBlockSchedule().apply {
            this.`val` = SoapVgUserBlockSchedule().apply {
                this.comment = "test"
                this.vgid = vgId
                this.timefrom = startDate.toString()
                this.timeto = endDate.toString()
                // this.parentid = 0L ???
            }
        },
        SetVgUserBlockScheduleResponse::class.java
    ).second.ret

    fun delVgUserBlockSchedule(
        sessionId: String,
        recordId: Long,
    ): Long = adapter.getResponseAsMandatoryType(
        sessionId,
        DelVgUserBlockSchedule().apply {
            this.id = recordId
        },
        DelVgUserBlockScheduleResponse::class.java
    ).second.ret

    fun promisePayment(
        sessionId: String,
        agreementId: Long,
        amount: Double
    ): Boolean {
        return adapter.getResponseAsMandatoryType(
            sessionId,
            ClientPromisePayment().apply {
                agrm = agreementId
                summ = amount
            },
            ClientPromisePaymentResponse::class.java
        ).second.ret == 1L
    }
}