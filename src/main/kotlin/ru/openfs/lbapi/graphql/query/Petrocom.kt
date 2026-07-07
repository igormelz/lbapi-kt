package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import ru.openfs.lbapi.domain.petrocom.PetrocomMigrationService

@GraphQLApi
class Petrocom(private val service: PetrocomMigrationService) {
    @Query
    fun loginPetrocom(
        @Name("login") login: String,
        @Name("password") password: String
    ) = service.loginFirst(login, password)
}