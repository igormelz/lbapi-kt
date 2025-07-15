package ru.openfs.lbapi.resource

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.model.PromiseSettings
import ru.openfs.lbapi.service.PromisePaymentService

@GraphQLApi
class PromisePaymentResource(
    private val service: PromisePaymentService,
) {
    @Query
    fun getPromiseSettings(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long
    ): PromiseSettings = service.getClientPromiseSettings(sessionId, agreementId)

    @Mutation
    fun promisePayment(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("amount") amount: Double
    ): Boolean = service.promisePayment(sessionId, agreementId, amount)
}
