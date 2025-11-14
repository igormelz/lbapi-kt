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
        @Name("sessionId") sessionId: String
    ) = service.getTariffsHistory(sessionId, 1354)
}