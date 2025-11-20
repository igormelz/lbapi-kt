package ru.openfs.lbapi.graphql.mutation

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import ru.openfs.lbapi.domain.payment.PaymentService
import ru.openfs.lbapi.domain.payment.model.PaymentConfirmation

@GraphQLApi
class Payment(
    private val service: PaymentService,
) {
    @Mutation
    fun payment(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("agreement") agreement: String,
        @Name("amount") amount: Double,
        @Name("sbp") sbp: Boolean?,
    ): PaymentConfirmation = service.payment(
        sessionId, agreementId, agreement, amount, sbp == true
    )
}