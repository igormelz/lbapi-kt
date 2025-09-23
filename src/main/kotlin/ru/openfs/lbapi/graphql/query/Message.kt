package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.message.MessageService

@GraphQLApi
class Message(
    private val service: MessageService,
) {

    @Query
    fun getSharedPosts(@Name("sessionId") sessionId: String) =
        service.getSharedPosts(sessionId)

    @Query
    fun getSharedPostsCat(@Name("sessionId") sessionId: String) =
        service.getSharedPostsCategories(sessionId)

    @Query
    fun getAccountSharedPostsCat(@Name("sessionId") sessionId: String) =
        service.getAccountSharedPostsCategories(sessionId)

}