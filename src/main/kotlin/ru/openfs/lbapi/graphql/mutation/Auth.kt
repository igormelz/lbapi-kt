package ru.openfs.lbapi.graphql.mutation

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import ru.openfs.lbapi.api3.Logout
import ru.openfs.lbapi.api3.LogoutResponse
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter

@GraphQLApi
class Auth(
    private val soapAdapter: SoapAdapter,
) {

    @Mutation
    fun signOut(
        @Name("sessionId") sessionId: String,
        @Name("login") login: String?,
    ) = soapAdapter.withSession(sessionId)
        .request<LogoutResponse> {
            Logout().also {
                Log.info("signOut [$login] with session:[$sessionId]")
            }
        }.ret

}