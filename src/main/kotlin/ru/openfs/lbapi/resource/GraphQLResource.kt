package ru.openfs.lbapi.resource

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import org.eclipse.microprofile.rest.client.inject.RestClient
import ru.openfs.lbapi.client.GoogleReCaptchaClient
import ru.openfs.lbapi.service.LbApiService

@GraphQLApi
class GraphQLResource(
    private val service: LbApiService,
    @RestClient private val googleReCaptchaClient: GoogleReCaptchaClient,
) {

    @Query
    fun getApiReady() = service.isApiReady()

    @Query
    fun login(@Name("login") login: String, @Name("password") password: String): String {
        Log.info("Try login: $login")

        val answer = service.startSession(login, password)

        Log.info("Successfully login $login with sessionId: [$answer]")
        return answer
    }

    @Query
    fun logout(@Name("sessionId") sessionId: String): String {
        Log.info("Try logout: $sessionId")

        service.closeSession(sessionId)

        Log.info("Successfully logout [$sessionId]")
        return "Bye"
    }

    @Query
    fun getAccounts(@Name("sessionId") sessionId: String) = service.getAccounts(sessionId)

    @Query
    fun getEmailIsConfirmed(@Name("login") login: String) = service.getAccountIsEmailConfirm(login)

    @Query
    fun getRecoveryPassword(@Name("login") login: String) = service.getRecoveryPassword(login)

    @Mutation
    fun updatePasswordByCode(
        @Name("login") login: String,
        @Name("password") password: String,
        @Name("code") code: String
    ): String? {
        Log.info("Try updatePassword for: $login by code: $code")
        return service.updatePasswordByCode(login, password, code)
    }

    @Mutation
    fun updatePasswordByToken(
        @Name("password") password: String,
        @Name("token") token: String
    ): String? {
        Log.info("Try updatePassword by token: $token")
        return service.updatePasswordByToken(password, token)
    }

    @Query
    fun verifyCode(@Name("code") code: String) =
        googleReCaptchaClient.verifyToken(code).success

    @Query
    fun getPasswordTemplate(@Name("sessionId") sessionId: String) =
        service.getPassTemplate(sessionId)

    @Mutation
    fun updatePassword(
        @Name("sessionId") sessionId: String,
        @Name("oldPass") oldPass: String,
        @Name("newPass") newPass: String
    ) = service.updatePassword(sessionId, oldPass, newPass)

    @Query
    fun getAccountNotices(@Name("sessionId") sessionId: String) =
        service.getAccountNotices(sessionId)

    @Query
    fun getSharedPosts(@Name("sessionId") sessionId: String) =
        service.getSharedPosts(sessionId)

    @Query
    fun getSharedPostsCat(@Name("sessionId") sessionId: String) =
        service.getSharedPostsCategories(sessionId)

    @Query
    fun getAccountSharedPostsCat(@Name("sessionId") sessionId: String) =
        service.getAccountSharedPostsCategories(sessionId)

    @Query
    fun getClientInfo(@Name("sessionId") sessionId: String) =
        service.getClientInfo(sessionId)

    @Query
    fun getAgreementsInfo(@Name("sessionId") sessionId: String) =
        service.getAgreementsInfo(sessionId)

    @Query
    fun getAgreementsStat(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long
    ) = service.getAgreementStat(sessionId, agreementId)

    @Query
    fun getAgreementInfo(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long
    ) = service.getAgreementsInfo(sessionId).filter { it.id == agreementId }

    @Query
    fun getPromiseSettings(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long
    ) = service.getClientPromiseSettings(sessionId, agreementId)

    @Query
    fun getPromisePayments(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("dateFrom") dateFrom: String?,
        @Name("dateTo") dateTo: String?,
    ) = service.getClientPromisePayments(sessionId, agreementId, dateFrom, dateTo)

    @Query
    fun getPayments(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("year") year: Int?,
    ) = service.getClientPayments(sessionId, agreementId, year)

}
