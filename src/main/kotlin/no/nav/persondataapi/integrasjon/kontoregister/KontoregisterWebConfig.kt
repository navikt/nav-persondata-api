package no.nav.persondataapi.integrasjon.kontoregister

import io.micrometer.common.KeyValues
import io.micrometer.observation.ObservationRegistry
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.*
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class KontoregisterWebConfig(private val obserservartionRegistry: ObservationRegistry) {
    @Value("\${KONTOREGISTER_URL}")
    lateinit var kontoregisterURL: String

    @Bean
    fun kontoregisterWebClient(
        builder: WebClient.Builder,
        @Qualifier("kontoregisterObservation")

        convention: ClientRequestObservationConvention,
        navCallIdHeaderFilter: ExchangeFilterFunction
    ): WebClient {

        // 1️⃣ Definer HttpClient med timeouts
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)              // 5 sek connect-timeout
            .responseTimeout(Duration.ofSeconds(30))                        // 30 sek response-time
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(10))                 // 10 sek read-timeout
                conn.addHandlerLast(WriteTimeoutHandler(10))                // 10 sek write-timeout
            }
            .metrics(true) { uri -> uri }

        return builder
            .baseUrl("$kontoregisterURL/api/system/v1/hent-konto-med-historikk")
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()
    }

    @Bean
    @Qualifier("kontoregisterObservation")
    fun kontoregisterObservationConvention(): ClientRequestObservationConvention =
        object : DefaultClientRequestObservationConvention() {
            override fun getLowCardinalityKeyValues(ctx: ClientRequestObservationContext): KeyValues {
                return super.getLowCardinalityKeyValues(ctx)
            }
        }

}
