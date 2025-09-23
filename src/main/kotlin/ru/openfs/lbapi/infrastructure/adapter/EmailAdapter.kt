package ru.openfs.lbapi.infrastructure.adapter

import io.quarkus.logging.Log
import io.quarkus.mailer.Mail
import io.quarkus.mailer.reactive.ReactiveMailer
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import java.io.IOException


@ApplicationScoped
class EmailAdapter(private val mailer: ReactiveMailer) {

    @Throws(IOException::class)
    fun sendEmail(name: String?): Uni<String> {
        Log.info("try sent email")
        val htmlBody = """
                 <html>
                                <body>
                                    <h1>Hello! ${name}</h1>
                                    <p>This is an HTML email with an inline image:</p>
                                    <img src="cid:logo@scrollnet"/>
                                </body>
                            </html>
                
                """.trimIndent()

        val imageData = javaClass.getClassLoader().getResourceAsStream("logo.png")
            ?.use { it.readAllBytes() }

        return mailer.send(
            Mail.withHtml(
                "igorm@openfs.ru",
                "rich EMAIL",
                htmlBody
            ).addInlineAttachment("logo", imageData, "image/png", "<logo@scrollnet>")
        ).flatMap {
            Uni.createFrom().item("email sent")
        }

    }
}
