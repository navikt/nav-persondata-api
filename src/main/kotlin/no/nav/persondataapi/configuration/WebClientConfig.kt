package no.nav.persondataapi.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.context.request.RequestContextListener
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Value("\${NAIS_TOKEN_EXCHANGE_ENDPOINT}")
    lateinit var tokenExchangeUrl: String

    @Bean
    fun tokenWebClient(builder: WebClient.Builder): WebClient =
        builder
            .baseUrl(tokenExchangeUrl)
            .defaultHeader("Content-Type", "application/json")
            .build()
    @Bean
    fun utbetalingWebClient(builder: WebClient.Builder): WebClient =
        builder
            .baseUrl("https://sokos-utbetaldata.dev.intern.nav.no/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern")
            .defaultHeader("Content-Type", "application/json")
            .build()
}
