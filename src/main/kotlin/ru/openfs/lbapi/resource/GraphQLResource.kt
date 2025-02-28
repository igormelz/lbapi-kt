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
    fun getAccount(@Name("sessionId") sessionId: String) = service.getAccounts(sessionId)

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
    fun verifyCode(@Name("code") code: String) = googleReCaptchaClient.verifyToken(code).success
}