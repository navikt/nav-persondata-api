package no.nav.persondataapi.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactive.awaitSingle
import no.nav.persondataapi.configuration.JsonUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

import org.springframework.web.reactive.function.client.WebClient

    @Component
class TokenService (
        @Qualifier("tokenWebClient")
        private val tokenWebClient: WebClient,
        private val objectMapper: ObjectMapper
    ) {
         fun exchangeToken(userToken: String, target: String): String {
        val requestBody = mapOf(
            "identity_provider" to "azuread",
            "target" to target,
            "user_token" to userToken
        )

            val v: JsonNode = JsonUtils.toJson(requestBody)
            println(v.toPrettyString())
        val response = tokenWebClient.post()
            .bodyValue(requestBody)
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