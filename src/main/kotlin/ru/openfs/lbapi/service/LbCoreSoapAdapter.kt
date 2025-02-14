package ru.openfs.lbapi.service

import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.apache.camel.ProducerTemplate
import org.eclipse.microprofile.rest.client.inject.RestClient
import ru.openfs.lbapi.camel.CamelRoute.Companion.CREATE_SOAP_MESSAGE
import ru.openfs.lbapi.camel.CamelRoute.Companion.PARSE_ERROR_MESSAGE
import ru.openfs.lbapi.camel.CamelRoute.Companion.READ_SOAP_MESSAGE
import ru.openfs.lbapi.client.LbCoreRestClient
import ru.openfs.lbapi.exception.ApiException
import ru.openfs.lbapi.exception.NotAuthorizeException

@ApplicationScoped
class LbCoreSoapAdapter(
    @RestClient private val restClient: LbCoreRestClient,
    private val producer: ProducerTemplate
) {

    fun getSessionId(request: Any): String =
        getResponse(null, request, String::class.java).first

    fun getResponseAsString(sessionId: String, request: Any): String =
        getResponse(sessionId, request, String::class.java).second

    fun <T> getResponse(
        sessionId: String?,
        request: Any,
        responseType: Class<T>,
    ): Pair<String, T> {
        val requestBody = producer.requestBody<String>(
            CREATE_SOAP_MESSAGE,
            request,
            String::class.java
        )
        try {
            restClient.call(sessionId ?: "", requestBody).use { response ->
                val responseBody = response.readEntity<String>(String::class.java)
                if (response.status != 200) {
                    val err = producer.requestBody<String>(
                        PARSE_ERROR_MESSAGE,
                        responseBody,
                        String::class.java
                    )

                    if (err == "Client not authorized") throw NotAuthorizeException(err)
                    throw ApiException(err)
                }
                return response.headers.getOrDefault("Set-Cookie", "").toString().getSessionId() to
                        producer.requestBody<T>(READ_SOAP_MESSAGE, responseBody, responseType)
            }
        } catch (e: RuntimeException) {
            Log.error(e.message)
            throw e
        }
    }

    private fun String.getSessionId() = this.split(";").first()

}