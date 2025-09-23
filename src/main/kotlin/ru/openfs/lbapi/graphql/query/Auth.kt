package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import ru.openfs.lbapi.api3.GetVersion
import ru.openfs.lbapi.api3.Logout
import ru.openfs.lbapi.api3.TVersion
import ru.openfs.lbapi.common.exception.ApiException

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
    ): String = soapAdapter.startClientSession(login, password)

    @Query
    fun logout(@Name("sessionId") sessionId: String): String =
        soapAdapter.withSession(sessionId).request<String> { Logout() }

}