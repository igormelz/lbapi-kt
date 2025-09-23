package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.account.AccountService
import ru.openfs.lbapi.domain.account.model.Account

@GraphQLApi
class Account(
    private val service: AccountService,
) {
    @Query
    fun getAccount(@Name("sessionId") sessionId: String): Account = service.getAccount(sessionId)
}