package ru.openfs.lbapi.resource

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.service.AccountService

@GraphQLApi
class AccountResource(
    private val service: AccountService,
) {
    @Query
    fun getAccounts(
        @Name("sessionId") sessionId: String
    ) = service.getAccounts(sessionId)
}
