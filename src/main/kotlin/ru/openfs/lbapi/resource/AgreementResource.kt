package ru.openfs.lbapi.resource

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.service.AgreementService

@GraphQLApi
class AgreementResource(
    private val service: AgreementService,
) {
    @Query
    fun getAgreementsInfo(@Name("sessionId") sessionId: String) =
        service.getAgreementsInfo(sessionId)

    @Query
    fun getAgreementsStat(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long
    ) = service.getAgreementStat(sessionId, agreementId)

    @Query
    fun getPayments(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("year") year: Int?,
    ) = service.getClientPayments(sessionId, agreementId, year)

}
