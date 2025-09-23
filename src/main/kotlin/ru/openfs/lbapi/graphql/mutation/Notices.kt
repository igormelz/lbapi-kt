package ru.openfs.lbapi.graphql.mutation

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import ru.openfs.lbapi.domain.notices.NoticesService
import ru.openfs.lbapi.domain.notices.model.UserNoticeInfo

@GraphQLApi
class Notices(
    private val service: NoticesService,
) {

    @Mutation
    fun setAccountNotice(
        @Name("sessionId") sessionId: String,
        @Name("uid") uid: Long,
        @Name("type") type: Long,
        @Name("isEmail") isEmail: Boolean,
        @Name("value") value: String?,
    ): Boolean = service.setAccountNotice(sessionId, uid, type, isEmail, value)

    @Mutation
    fun setAccountNotices(
        @Name("sessionId") sessionId: String,
        @Name("uid") uid: Long,
        @Name("notices") notices: List<UserNoticeInfo>,
    ): Boolean = service.setAccountNotices(sessionId, uid, notices)

}