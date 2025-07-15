package ru.openfs.lbapi.resource

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.service.PasswordService

@GraphQLApi
class PasswordResource(
    private val service: PasswordService,
) {

    @Query
    fun getEmailIsConfirmed(
        @Name("login") login: String
    ): Boolean {
        Log.info("try test email confirmed for $login")
        val answer = service.getAccountIsEmailConfirm(login)
        if (answer) Log.info("Successfully get email confirmed for $login") else Log.info("Fail to get email confirmed for $login")
        return answer
    }

    @Query
    fun getRecoveryPassword(
        @Name("login") login: String
    ) = service.getRecoveryPassword(login)

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
    fun getPasswordTemplate(
        @Name("sessionId") sessionId: String
    ) = service.getPassTemplate(sessionId)

    @Mutation
    fun updatePassword(
        @Name("sessionId") sessionId: String,
        @Name("oldPass") oldPass: String,
        @Name("newPass") newPass: String
    ) = service.updatePassword(sessionId, oldPass, newPass)

}
