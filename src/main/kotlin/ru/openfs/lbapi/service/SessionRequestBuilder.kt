package ru.openfs.lbapi.service

import ru.openfs.lbapi.service.adapter.SoapAdapter

class SessionRequestBuilder(
    private val adapter: SoapAdapter,
    private val sessionId: String? = null,
) {

    internal inline fun <reified Response : Any> request(
        noinline requestBuilder: () -> Any
    ): Response {
        return adapter.getResponseAsMandatoryType(
            sessionId,
            requestBuilder(),
            Response::class.java
        ).second
    }


}