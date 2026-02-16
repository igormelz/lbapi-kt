package ru.openfs.lbapi.graphql.mutation

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import ru.openfs.lbapi.domain.blocking.BlockingService
import java.time.Instant
import java.time.ZoneId

@GraphQLApi
class UserBlock(
    private val service: BlockingService,
) {
    @Mutation
    fun setUserBlock(
        @Name("sessionId") sessionId: String,
        @Name("vgId") vgId: Long,
        @Name("startDate") startDate: String,
        @Name("endDate") endDate: String,
    ): Long {
        val fromDate = Instant.parse(startDate).atZone(ZoneId.of("Europe/Moscow")).toLocalDate()
        val toDate = Instant.parse(endDate).atZone(ZoneId.of("Europe/Moscow")).toLocalDate()
        Log.info("try block: ${vgId} from: $startDate [$fromDate], to: $endDate [$toDate]")
        return service.setUserBlockSchedule(sessionId, vgId, fromDate, toDate)
    }

    @Mutation
    fun removeUserBlock(
        @Name("sessionId") sessionId: String,
        @Name("recordId") id: Long
    ) = service.delVgUserBlockSchedule(sessionId, id)
}