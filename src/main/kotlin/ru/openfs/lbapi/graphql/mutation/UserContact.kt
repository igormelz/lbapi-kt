package ru.openfs.lbapi.graphql.mutation

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import ru.openfs.lbapi.domain.contact.UserContactService

@GraphQLApi
class UserContact(
    private val service: UserContactService,
) {

    @Mutation
    fun upsertEmail(
        @Name("sessionId") sessionId: String,
        @Name("email") email: String
    ): Boolean = service.upsertEmail(sessionId, email)

    @Mutation
    fun confirmValidateEmail(
        @Name("sessionId") sessionId: String,
        @Name("id") recordId: Long,
        @Name("code") code: String,
    ): Boolean = service.confirmValidateEmail(sessionId, recordId, code)

    @Mutation
    fun upsertMobile(
        @Name("sessionId") sessionId: String,
        @Name("mobile") mobile: String
    ): Boolean = service.upsertMobile(sessionId, mobile)

}