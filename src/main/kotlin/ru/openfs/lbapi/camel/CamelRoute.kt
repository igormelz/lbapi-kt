package ru.openfs.lbapi.camel

import jakarta.inject.Singleton
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.dataformat.soap.name.ServiceInterfaceStrategy
import org.apache.camel.model.dataformat.SoapDataFormat
import ru.openfs.lbapi.api3.Api3PortType

@Singleton
class CamelRoute : RouteBuilder() {

    override fun configure() {

        val soap = SoapDataFormat(
            "ru.openfs.lbapi.api3",
            ServiceInterfaceStrategy(Api3PortType::class.java, true)
        )

        // marshal object to soap
        from(CREATE_SOAP_MESSAGE)
            .id("MarshalSoapMessage")
            .marshal(soap)

        // unmarshal byte[] to object
        from(READ_SOAP_MESSAGE)
            .id("UnmarshalSoapMessage")
            .unmarshal(soap)

        // process fault
        from(PARSE_ERROR_MESSAGE)
            .id("GetFaultMessage")
            .transform(
                xpath(
                    "//detail/text()",
                    String::class.java
                )
            )
    }

    companion object {
        const val CREATE_SOAP_MESSAGE = "direct:marshalSoap"
        const val READ_SOAP_MESSAGE = "direct:unmarshalSoap"
        const val PARSE_ERROR_MESSAGE = "direct:getFaultMessage"
    }
}