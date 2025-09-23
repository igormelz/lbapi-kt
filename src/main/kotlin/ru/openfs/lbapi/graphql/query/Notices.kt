package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.notices.NoticesService
import ru.openfs.lbapi.domain.notices.model.UserNoticeInfo

@GraphQLApi
class Notices(
    private val service: NoticesService,
) {

    @Query
    fun getAccountNotices(
        @Name("sessionId") sessionId: String,
        @Name("uid") uid: Long
    ): List<UserNoticeInfo> = service.getAccountNotices(sessionId, uid)

}