package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.contact.UserContactService
import ru.openfs.lbapi.infrastructure.adapter.CaptchaAdapter

@GraphQLApi
class UserContact(
    private val service: UserContactService,
    private val captchaAdapter: CaptchaAdapter,
) {

    @Query
    fun getEmailIsConfirmed(@Name("login") login: String, @Name("payload") payload: String): Boolean =
       captchaAdapter.verify(payload) && service.getAccountIsEmailConfirm(login)

    @Query
    fun validateEmail(@Name("sessionId") sessionId: String): Long = service.validateEmail(sessionId)

    @Query
    fun isEmailReady(sessionId: String): Boolean = service.isEmailReady(sessionId)

    @Query
    fun altchaChallenge() = captchaAdapter.getChallenge().toJson()


}