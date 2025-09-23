package ru.openfs.lbapi.graphql.query

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import org.eclipse.microprofile.rest.client.inject.RestClient
import ru.openfs.lbapi.infrastructure.client.GoogleReCaptchaClient

@GraphQLApi
class Captcha(
    @param:RestClient private val googleReCaptchaClient: GoogleReCaptchaClient,
) {
    @Query
    fun verifyCode(@Name("code") code: String): Boolean {
        return googleReCaptchaClient.verifyToken(code).success == true
    }
}