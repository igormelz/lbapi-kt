package ru.openfs.lbapi.graphql.mutation

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Mutation
import org.eclipse.microprofile.graphql.Name
import ru.openfs.lbapi.domain.petrocom.PetrocomMigrationService

@GraphQLApi
class Petrocom(private val service: PetrocomMigrationService) {
    @Mutation
    fun agreePetrocom(
        @Name("sessionId") sessionId: String,
        @Name("login") login: String,
    ) = service.agree(sessionId, login)
}