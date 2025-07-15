package ru.openfs.lbapi.resource

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.service.MessageService

@GraphQLApi
class MessageResource(
    private val service: MessageService,
) {

    @Query
    fun getAccountNotices(@Name("sessionId") sessionId: String) =
        service.getAccountNotices(sessionId)

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
