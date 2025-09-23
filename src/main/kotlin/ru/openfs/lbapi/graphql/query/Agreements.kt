package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.agreement.AgreementService
import ru.openfs.lbapi.domain.agreement.model.AgreementInfo

@GraphQLApi
class Agreements(
    private val service: AgreementService,
) {
    @Query
    fun getAgreementsInfo(@Name("sessionId") sessionId: String): List<AgreementInfo> =
        service.getAgreementsInfo(sessionId)
}