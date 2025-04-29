package ru.openfs.lbapi.service

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.exception.ApiException
import ru.openfs.lbapi.model.*
import ru.openfs.lbapi.service.adapter.LbCoreSoapAdapter
import ru.openfs.lbapi.utils.FormatUtil.convertBpsToReadableFormat
import java.time.LocalDate
import java.time.Month

@ApplicationScoped
class LbApiService(private val adapter: LbCoreSoapAdapter) {

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

    fun getClientVGroups(sessionId: String): List<SoapClientVgroupFull> =
        adapter.getResponseAsMandatoryType(
            sessionId,
            GetClientVgroups(),
            GetClientVgroupsResponse::class.java
        ).second.ret

    // get agreements info
    fun getAgreementsInfo(sessionId: String): List<AgreementInfo> =
        getAccounts(sessionId).agreements.map { agreement ->
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
                adapter
                    .getClientVGroupByAgreement(sessionId, agreement.agrmid)
                    // keep inet and onetime
                    .filter { it.vgroup.tariftype == 1L || it.vgroup.tariftype == 5L }
                    .map {
                        ServiceInfo(
                            id = it.vgroup.vgid,
                            login = it.vgroup.login,
                            address = it.addresses.first().address,
                            tarName = it.vgroup.tarifdescr,
                            tarShape = convertBpsToReadableFormat(it.vgroup.curshape),
                            tarRent = it.vgroup.servicerent,
                            blocked = it.vgroup.blocked != 0L,
                            blockedType = it.vgroup.blocked,
                            tarType = it.vgroup.tariftype,
                            rentPeriod = when {
                                it.vgroup.tarifdescr.contains("полугодовой") -> "за 6 мес."
                                it.vgroup.tarifdescr.contains("годовой") -> "за 12 мес."
                                else -> "в мес."
                            }
                        )
                    },
            )
        }

    fun getClientPromiseSettings(sessionId: String, agreementId: Long): PromiseSettings {
        val recPayment = adapter.getRecommendedPayment(sessionId, agreementId, 0L)
        return adapter.getResponseAsMandatoryType(
            sessionId,
            GetClientPPSettings().apply { this.agrm = agreementId },
            GetClientPPSettingsResponse::class.java
        ).second.ret.first().let {
            PromiseSettings(
                isAllowed = it.promiseavailable != 1L,
//                        (p.balance < 0 && abs(p.balance) > it.promiselimit) -> false
//                        (p.isCredit) -> false
                dateLimit = LocalDate.now().plusDays(it.promisetill),
                maxAmount = it.promisemax,
                minAmount = it.promisemin,
                recAmount = if (recPayment < it.promisemin) {
                    it.promisemin
                } else if (recPayment > it.promisemax) {
                    it.promisemax
                } else {
                    recPayment
                },
                limitAmount = it.promiselimit
            )
        }
    }

    fun getClientPromisePayments(
        sessionId: String,
        agreementId: Long,
        dateFrom: String?,
        dateTo: String?
    ): List<SoapPromisePayment> {
        return adapter.getResponseAsMandatoryType(
            sessionId,
            GetClientPromisePayments().apply {
                this.flt = SoapFilter().apply {
                    this.agrmid = agreementId
                    this.dtfrom = dateFrom ?: LocalDate.now().minusMonths(1L).toString()
                    this.dtto = dateTo ?: LocalDate.now().toString()
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

}