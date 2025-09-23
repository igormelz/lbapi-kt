package ru.openfs.lbapi.graphql.mutation

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import ru.openfs.lbapi.domain.password.UserPasswordService

@GraphQLApi
class UserPassword(
    private val service: UserPasswordService,
) {

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

    @Mutation
    fun updatePassword(
        @Name("sessionId") sessionId: String,
        @Name("oldPass") oldPass: String,
        @Name("newPass") newPass: String
    ): Boolean = service.updatePassword(sessionId, oldPass, newPass)

}