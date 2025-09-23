package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.payment.PaymentService
import ru.openfs.lbapi.domain.payment.model.PaymentInfo

@GraphQLApi
class Payment(
    private val service: PaymentService,
) {
    @Query
    fun getPayments(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
        @Name("year") year: Int?,
    ): List<PaymentInfo> = service.getClientPayments(sessionId, agreementId, year)
}