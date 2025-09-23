package ru.openfs.lbapi.graphql.mutation

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import ru.openfs.lbapi.domain.promisepayment.PromisePaymentService

@GraphQLApi
class PromisePayment(
    private val service: PromisePaymentService,
) {
    @Mutation
    fun promisePayment(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("amount") amount: Double
    ): Boolean = service.promisePayment(sessionId, agreementId, amount)
}