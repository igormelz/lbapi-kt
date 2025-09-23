package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.password.UserPasswordService
import ru.openfs.lbapi.domain.password.model.PasswordTemplate

@GraphQLApi
class UserPassword(
    private val service: UserPasswordService,
) {

    @Query
    fun getRecoveryPassword(@Name("login") login: String): Long = service.getRecoveryPassword(login).recordid

    @Query
    fun getPasswordTemplate(@Name("sessionId") sessionId: String): PasswordTemplate = service.getPassTemplate(sessionId)

}