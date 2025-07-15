package ru.openfs.lbapi.resource

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.service.BlockingService
import java.time.Instant
import java.time.ZoneOffset

@GraphQLApi
class BlockingResource(
    private val service: BlockingService,
) {
    @Query
    fun getUserBlockTemplate(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("vgId") vgId: Long
    ) = service.getUserBlockTemplate(sessionId, agreementId, vgId)

    @Query
    fun getUserBlockSchedule(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("vgId") vgId: Long
    ) = service.getVgUserBlockSchedule(sessionId, agreementId, vgId)

    @Mutation
    fun setUserBlock(
        @Name("sessionId") sessionId: String,
        @Name("vgId") vgId: Long,
        @Name("startDate") startDate: String,
        @Name("endDate") endDate: String,
    ): Long {
        val fromDate = Instant.parse(startDate).atZone(ZoneOffset.systemDefault()).toLocalDate()
        val toDate = Instant.parse(endDate).atZone(ZoneOffset.systemDefault()).toLocalDate()
        Log.info("try block: ${vgId} from: $startDate [$fromDate], to: $endDate [$toDate]")
        return service.setVgUserBlockSchedule(sessionId, vgId, fromDate, toDate)
    }

    @Mutation
    fun removeUserBlock(
        @Name("sessionId") sessionId: String,
        @Name("recordId") id: Long
    ) = service.delVgUserBlockSchedule(sessionId, id)
}
