package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.tarif.TariffService

@GraphQLApi
class Tariff(
    private val service: TariffService,
) {
    
    @Query
    fun getTariffsHistory(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long
    ) = service.getTariffsHistory(sessionId, agreementId)

    @Query
    fun getTariffsStaff(
        @Name("sessionId") sessionId: String,
        @Name("tarId") tarId: Long
    ) = service.getTariffsStaff(sessionId, tarId)


    @Query
    fun getAvailableTariffs(
        @Name("sessionId") sessionId: String,
        @Name("vgId") vgId: Long,
        @Name("currentRent") currentRent: Double,
    ) = service.getTariffByServiceId(sessionId, vgId, currentRent)

}