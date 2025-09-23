package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.promisepayment.PromisePaymentService
import ru.openfs.lbapi.domain.promisepayment.model.PromiseSettings

@GraphQLApi
class PromisePayment(
    private val service: PromisePaymentService,
) {
    @Query
    fun getPromiseSettings(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long
    ): PromiseSettings = service.getClientPromiseSettings(sessionId, agreementId)
}