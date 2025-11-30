package ru.openfs.lbapi.graphql.mutation

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import ru.openfs.lbapi.domain.tarif.TariffService
import java.time.Instant
import java.time.ZoneOffset

@GraphQLApi
class Tariff(
    private val service: TariffService,
) {
    @Mutation
    fun setChangeTariff(
        @Name("sessionId") sessionId: String,
        @Name("vgId") vgId: Long,
        @Name("startDate") startDate: String,
        @Name("agentId") agentId: Long,
        @Name("currentTarId") tarIdOld: Long,
        @Name("tarId") tarIdNew: Long,
        @Name("serviceCat") serviceCat: Long?
    ): Long {
        val fromDate = Instant.parse(startDate).atZone(ZoneOffset.systemDefault()).toLocalDate()
        Log.info("try change tariff ${vgId} from: $fromDate to: $tarIdNew")
        return service.addTariffSchedule(sessionId, vgId, agentId, tarIdOld, tarIdNew, fromDate, serviceCat)
    }

//    @Mutation
//    fun removeUserBlock(
//        @Name("sessionId") sessionId: String,
//        @Name("recordId") id: Long
//    ) = service.delVgUserBlockSchedule(sessionId, id)
}