package ru.openfs.lbapi.service.adapter

import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.apache.camel.ProducerTemplate
import org.eclipse.microprofile.rest.client.inject.RestClient
import ru.openfs.lbapi.api3.ClientLogin
import ru.openfs.lbapi.camel.CamelRoute
import ru.openfs.lbapi.client.LbCoreRestClient
import ru.openfs.lbapi.exception.*

@ApplicationScoped
class SoapAdapter(
    @param:RestClient private val restClient: LbCoreRestClient,
    private val producer: ProducerTemplate
) {

    fun withouSession(): SessionRequestBuilder {
        return SessionRequestBuilder(this)
    }

    fun withSession(sessionId: String): SessionRequestBuilder {
        return SessionRequestBuilder(this, sessionId)
    }

    fun startClientSession(login: String, password: String): String =
        getResponseAsMandatoryType(
            null,
            ClientLogin().apply {
                this.login = login
                this.pass = password
            },
            String::class.java
        ).first ?: throw NotAuthorizeException("not return sessionId")

    fun <T> getResponseAsMandatoryType(
        sessionId: String?,
        request: Any,
        responseType: Class<T>,
    ): Pair<String?, T> {
        val requestBody = producer.requestBody(
            CamelRoute.CREATE_SOAP_MESSAGE,
            request,
            String::class.java
        )
        try {
            restClient.call(sessionId, requestBody).use { response ->
                val responseBody = response.readEntity(String::class.java)

                if (response.status != 200) {
                    val err = producer.requestBody(
                        CamelRoute.PARSE_ERROR_MESSAGE,
                        responseBody,
                        String::class.java
                    )

                    when (err) {
                        "Promise payments already assigned" ->
                            throw PromisePaymentNotAllowedException(err)

                        "Client not authorized" ->
                            throw NotAuthorizeException(err)

                        "Account not found" ->
                            throw NotfoundAccountException(err)

                        "Promise payment is not available, last payment is overdue" ->
                            throw PromisePaymentOverdueException(err)

                        else -> throw ApiException(err)
                    }
                }

                return response.cookies["sessnum"]?.value?.getSessionId() to
                        producer.requestBody<T>(CamelRoute.READ_SOAP_MESSAGE, responseBody, responseType)
            }
        } catch (e: RuntimeException) {
            Log.error(e.message)
            throw e
        }
    }

    private fun String.getSessionId() = this.split(";").first()

}