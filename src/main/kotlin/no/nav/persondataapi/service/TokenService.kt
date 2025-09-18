package no.nav.persondataapi.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactive.awaitSingle
import no.nav.persondataapi.configuration.JsonUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters

import org.springframework.web.reactive.function.client.WebClient

    @Component
class TokenService (
        @Qualifier("tokenWebClient")
        private val tokenWebClient: WebClient,
        @Qualifier("azuretokenWebClient")
        private val azuretokenWebClient: WebClient,
        private val objectMapper: ObjectMapper,
        private val environment: Environment
    ) {
        fun exchangeToken(userToken: String, target: String): String {
        val requestBody = mapOf(
            "identity_provider" to "azuread",
            "target" to target,
            "user_token" to userToken
        )
        val response = tokenWebClient.post()
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(TokenResponse::class.java)
            .block() // Bruk `awaitSingle()` hvis du er i `suspend`-verden

        return response?.access_token
            ?: throw IllegalStateException("Access token mangler i token-respons")
    }
        fun exchangeToken(userToken: String, scope: SCOPE): String {
            val target = environment[scope.toString()]
            val requestBody = mapOf(
                "identity_provider" to "azuread",
                "target" to target,
                "user_token" to userToken
            )
            val response = tokenWebClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(TokenResponse::class.java)
                .block() // Bruk `awaitSingle()` hvis du er i `suspend`-verden

            return response?.access_token
                ?: throw IllegalStateException("Access token mangler i token-respons")
        }
        fun getServiceToken(scope: SCOPE): String {
            val target = environment[scope.toString()]
            val clientId = environment["AZURE_APP_CLIENT_ID"]
            val clientSecret = environment["AZURE_APP_CLIENT_SECRET"]
            val response = azuretokenWebClient.post()
                .body(
                    BodyInserters.fromFormData("client_id", clientId!!)
                        .with("scope", target!!)
                        .with("client_secret", clientSecret!!)
                        .with("grant_type", "client_credentials"))

                .retrieve()
                .bodyToMono(TokenResponse::class.java)
                .block() // Bruk `awaitSingle()` hvis du er i `suspend`-verden

            return response?.access_token
                ?: throw IllegalStateException("Access token mangler i token-respons")
        }
}
data class TokenResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String
)

enum class SCOPE {
    PDL_SCOPE, UTBETALING_SCOPE, TILGANGMASKIN_SCOPE ,AAREG_SCOPE, INNTEKT_SCOPE, KODEVERK_SCOPE
}
