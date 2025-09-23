package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.blocking.BlockingService
import ru.openfs.lbapi.domain.blocking.model.UserBlockSchedule
import ru.openfs.lbapi.domain.blocking.model.UserBlockTemplate

@GraphQLApi
class Blocking(
    private val service: BlockingService,
) {
    @Query
    fun getUserBlockTemplate(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("vgId") vgId: Long
    ): UserBlockTemplate? = service.getUserBlockTemplate(sessionId, agreementId, vgId)

    @Query
    fun getUserBlockSchedule(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("vgId") vgId: Long
    ): List<UserBlockSchedule> = service.getUserBlockSchedules(sessionId, agreementId, vgId)
}