package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.contact.UserContactService

@GraphQLApi
class UserContact(
    private val service: UserContactService,
) {

    @Query
    fun getEmailIsConfirmed(@Name("login") login: String): Boolean = service.getAccountIsEmailConfirm(login)

    @Query
    fun validateEmail(@Name("sessionId") sessionId: String): Long = service.validateEmail(sessionId)

    @Query
    fun isEmailReady(sessionId: String): Boolean = service.isEmailReady(sessionId)

}