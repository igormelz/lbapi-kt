package ru.openfs.lbapi.resource

import io.quarkus.logging.Log
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Name
import org.eclipse.microprofile.graphql.Query
import org.eclipse.microprofile.rest.client.inject.RestClient
import ru.openfs.lbapi.client.GoogleReCaptchaClient

@GraphQLApi
class CaptchaResource(
    @RestClient private val googleReCaptchaClient: GoogleReCaptchaClient,
) {
    @Query
    fun verifyCode(@Name("code") code: String): Boolean {
        val ggl = googleReCaptchaClient.verifyToken(code)
        Log.info("Successfully verify code: $ggl")
        return ggl.success == true
    }
}
