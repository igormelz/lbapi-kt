package ru.openfs.lbapi.infrastructure.client

import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.rest.client.reactive.ClientFormParam
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/recaptcha/api/siteverify")
@RegisterRestClient(configKey = "recaptcha")
fun interface GoogleReCaptchaClient {
    @POST
    @ClientFormParam(name = "secret", value = ["\${google.server-key}"])
    fun verifyToken(@FormParam("response") token: String): GoogleReCaptchaResponse
}

data class GoogleReCaptchaResponse(
    val success: Boolean?,
    val hostname: String?,
    @JsonProperty("error_codes")
    val errorCodes: List<String>?,
)