package ru.openfs.lbapi.infrastructure.adapter

import jakarta.enterprise.context.ApplicationScoped
import org.altcha.altcha.v2.Altcha
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class CaptchaAdapter(
    @param:ConfigProperty(name = "captcha.secret")
    private val secret: String
) {
    fun getChallenge(): Altcha.Challenge {
        val option = Altcha.CreateChallengeOptions()
            .algorithm("PBKDF2/SHA-256")
            .cost(5_000)
            .hmacSignatureSecret(secret)
            .expiresInSeconds(600)
        return Altcha.createChallenge(option)
    }

    fun verify(base64payload: String): Boolean {
        val result = Altcha.verifySolution(
            base64payload, secret, Altcha.kdf("PBKDF2/SHA-256"))
        return result.verified()
    }
}