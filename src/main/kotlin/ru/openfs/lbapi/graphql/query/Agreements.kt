package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.agreement.AgreementService
import ru.openfs.lbapi.domain.agreement.model.Agreement
import ru.openfs.lbapi.domain.agreement.model.AgreementInfo

@GraphQLApi
class Agreements(
    private val service: AgreementService,
) {

    @Query
    fun getAgreements(
        @Name("sessionId") sessionId: String,
        @Name("login") login: String?,
    ): List<Agreement> = service.getAgreements(sessionId, login)

    @Query
    fun getAgreement(
        @Name("sessionId") sessionId: String,
        @Name("login") login: String?,
        @Name("number") agreementNumber: String,
    ): AgreementInfo = service.getAgreementInfo(sessionId, login, agreementNumber)


    @Query
    fun getAgreementsInfo(
        @Name("sessionId") sessionId: String,
        @Name("login") login: String?,
    ): List<AgreementInfo> = service.getAgreementsInfo(sessionId, login)

}