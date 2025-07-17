package ru.openfs.lbapi.resource

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.api3.GetVersion
import ru.openfs.lbapi.api3.Logout
import ru.openfs.lbapi.api3.TVersion
import ru.openfs.lbapi.exception.ApiException
import ru.openfs.lbapi.service.adapter.SoapAdapter

@GraphQLApi
class AuthResource(
    private val soapAdapter: SoapAdapter,
) {

    @Query
    fun getApiReady(): Boolean = try {
        soapAdapter.noSession().request<TVersion> { GetVersion() }.version.isNotEmpty()
    } catch (_: ApiException) {
        false
    }

    @Query
    fun login(
        @Name("login") login: String,
        @Name("password") password: String
    ): String {
        Log.info("Try login: $login")
        val sessionId = soapAdapter.startClientSession(login, password)
        Log.info("Successfully login $login with sessionId: [$sessionId]")
        return sessionId
    }

    @Query
    fun logout(
        @Name("sessionId") sessionId: String
    ): String {
        Log.info("Try logout: $sessionId")
        soapAdapter.withSession(sessionId).request<String> { Logout() }
        Log.info("Successfully logout [$sessionId]")
        return "Bye"
    }

}
