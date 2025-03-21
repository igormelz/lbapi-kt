package ru.openfs.lbapi.service.adapter

import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.apache.camel.ProducerTemplate
import org.eclipse.microprofile.rest.client.inject.RestClient
import ru.openfs.lbapi.camel.CamelRoute
import ru.openfs.lbapi.client.LbCoreRestClient
import ru.openfs.lbapi.exception.ApiException
import ru.openfs.lbapi.exception.NotAuthorizeException
import ru.openfs.lbapi.exception.NotfoundAccountException
import ru.openfs.lbapi.exception.PromisePaymentNotAllowedException

@ApplicationScoped
class LbCoreSoapAdapter(
    @RestClient private val restClient: LbCoreRestClient,
    private val producer: ProducerTemplate
) {

    fun getSessionId(request: Any): String =
        getResponseAsMandatoryType(null, request, String::class.java).first
            ?: throw NotAuthorizeException("not return sessionId")

    fun getResponseAsString(sessionId: String, request: Any): String =
        getResponseAsMandatoryType(sessionId, request, String::class.java).second

    fun <T> getResponseAsMandatoryType(
        sessionId: String?,
        request: Any,
        responseType: Class<T>,
    ): Pair<String?, T> {
        val requestBody = producer.requestBody<String>(
            CamelRoute.Companion.CREATE_SOAP_MESSAGE,
            request,
            String::class.java
        )
        try {
            restClient.call(sessionId, requestBody).use { response ->
                val responseBody = response.readEntity<String>(String::class.java)

                if (response.status != 200) {
                    val err = producer.requestBody<String>(
                        CamelRoute.Companion.PARSE_ERROR_MESSAGE,
                        responseBody,
                        String::class.java
                    )

                    when (err) {
                        "Promise payments already assigned" -> throw PromisePaymentNotAllowedException(err)
                        "Client not authorized" -> throw NotAuthorizeException(err)
                        "Account not found" -> throw NotfoundAccountException(err)
                        else -> throw ApiException(err)
                    }
                }

                return response.cookies["sessnum"]?.value?.getSessionId() to
                        producer.requestBody<T>(CamelRoute.Companion.READ_SOAP_MESSAGE, responseBody, responseType)
            }
        } catch (e: RuntimeException) {
            Log.error(e.message)
            throw e
        }
    }

    private fun String.getSessionId() = this.split(";").first()

}