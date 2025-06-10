package ru.openfs.lbapi.resource

import graphql.Mutable
import io.smallrye.common.annotation.Blocking
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.service.LbApiService
import ru.openfs.lbapi.service.adapter.LbCoreSoapAdapter

@GraphQLApi
class DevGraphQLResource(
    private val adapter: LbCoreSoapAdapter,
    private val service: LbApiService,
) {
    @Query
    fun devUsbox(
        @Name("sessionId") sessionId: String,
        @Name("vgId") vgId: Long
    ) = adapter.getUsboxService(sessionId, vgId)

    @Query
    fun devVgroupService(
        @Name("sessionId") sessionId: String,
        @Name("vgId") vgId: Long
    ) = adapter.getClientVgroupService(sessionId, vgId)

    @Query
    fun devVgroup(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long
    ) = adapter.getClientVGroupByAgreement(sessionId, agreementId)

    @Mutation
    fun devEmail() = service.sentEmailConfirmed()

    @Query
    fun devPromisePayments(
        @Name("sessionId") sessionId: String,
        @Name("agreementId") agreementId: Long,
    ) = service.getClientPromisePayments(sessionId, agreementId)
}
