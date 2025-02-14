package ru.openfs.lbapi.resource

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.service.LbApiService

@GraphQLApi
class GraphQLResource(private val service: LbApiService) {

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
    fun getAccount(@Name("sessionId") sessionId: String) = service.getAccounts(sessionId).ret

}