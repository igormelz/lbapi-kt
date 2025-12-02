package ru.openfs.lbapi.graphql.query

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.api3.GetVersion
import ru.openfs.lbapi.api3.Logout
import ru.openfs.lbapi.api3.LogoutResponse
import ru.openfs.lbapi.api3.TVersion
import ru.openfs.lbapi.common.exception.ApiException
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter

@GraphQLApi
class Auth(
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
    ): String = soapAdapter.startClientSession(login, password).also {
        Log.info("logged as [$login] with session:[$it]")
    }


    @Query
    fun logout(
        @Name("sessionId") sessionId: String,
        @Name("login") login: String?,
    ): String =
        soapAdapter.withSession(sessionId)
            .request<LogoutResponse> {
                Logout().also {
                    Log.info("logout [$login] with session:[$sessionId]")
                }
            }.ret.toString()

}