package no.nav.persondataapi.konfigurasjon

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import io.micrometer.observation.ObservationRegistry
import io.netty.resolver.DefaultAddressResolverGroup
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientRequest

import org.springframework.web.reactive.function.client.ClientRequestObservationConvention
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.util.UUID

@Configuration
class WebClientConfig(private val observationRegistry: ObservationRegistry) {

    @Value("\${NAIS_TOKEN_EXCHANGE_ENDPOINT}")
    lateinit var tokenExchangeUrl: String
    @Value("\${AZURE_OPENID_CONFIG_TOKEN_ENDPOINT}")
    lateinit var azureTokenEndpointUrl: String
    @Value("\${UTBETALING_URL}")
    lateinit var utbetalingURL: String
    @Value("\${INNTEKT_URL}")
    lateinit var inntektURL: String
    @Value("\${TILGANGMASKIN_URL}")
    lateinit var tilgangmaskinURL: String
    @Value("\${AAREG_URL}")
    lateinit var aaregURL: String
    @Value("\${EREG_URL}")
    lateinit var eregURL: String
    @Value("\${KODEVERK_URL}")
    lateinit var kodeverkURL: String

    @Bean
    fun tokenWebClient(builder: WebClient.Builder): WebClient =
        builder
            .baseUrl(tokenExchangeUrl)
            .defaultHeader("Content-Type", "application/json")
            .build()
    @Bean
    fun azuretokenWebClient(builder: WebClient.Builder): WebClient =
        builder
            .baseUrl(azureTokenEndpointUrl)
            .defaultHeader("Content-Type", "application/json")
            .build()
    @Bean
    fun utbetalingWebClient(
                    builder: WebClient.Builder,
                    @Qualifier("utbetalingObservation")
                    convention: ClientRequestObservationConvention,
                    navCallIdHeaderFilter: ExchangeFilterFunction
    ): WebClient =
        builder
            .baseUrl("$utbetalingURL/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern")
            //.defaultHeader("Content-Type", "application/json")
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create().metrics(true,
                        java.util.function.Function<String, String> { uri ->
                            // Returnér hva du vil tagge som "uri" (f.eks. masker variabler)
                            uri
                        })
                )
            )
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()

    @Bean
    fun inntektWebClient(builder: WebClient.Builder,
                         @Qualifier("inntektObservation")
                         convention: ClientRequestObservationConvention,
                         navCallIdHeaderFilter: ExchangeFilterFunction): WebClient =
        builder
            .baseUrl(inntektURL)
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create().metrics(true,
                        java.util.function.Function<String, String> { uri ->
                            // Returnér hva du vil tagge som "uri" (f.eks. masker variabler)
                            uri
                        })
                )
            )
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()

    @Bean
    fun tilgangWebClient(builder: WebClient.Builder,
                         navCallIdHeaderFilter: ExchangeFilterFunction): WebClient =
        builder
            .baseUrl(tilgangmaskinURL)
            //.defaultHeader("Content-Type", "application/json")
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .filter(navCallIdHeaderFilter)
            .build()
    @Bean
    fun aaregWebClient(builder: WebClient.Builder,
                       @Qualifier("aaregObservation")
                       convention: ClientRequestObservationConvention,
                       navCallIdHeaderFilter: ExchangeFilterFunction): WebClient =
        builder
            .baseUrl(aaregURL)
            //.defaultHeader("Content-Type", "application/json")
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }.clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create().metrics(true,
                        java.util.function.Function<String, String> { uri ->
                            // Returnér hva du vil tagge som "uri" (f.eks. masker variabler)
                            uri
                        })
                )
            )
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()

    @Bean
    fun eregWebClient(builder: WebClient.Builder,
                      navCallIdHeaderFilter: ExchangeFilterFunction): WebClient =
        builder
            .baseUrl(eregURL)
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .filter(navCallIdHeaderFilter)
            .build()

    @Bean
    @Qualifier("pdlWebClient")
    fun pdlWebClient(
        base: WebClient.Builder,
        @Qualifier("pdlObservation")
        convention: ClientRequestObservationConvention,
        navCallIdHeaderFilter: ExchangeFilterFunction
    ): WebClient =
        base.clone()
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create().metrics(
                        true,
                        java.util.function.Function<String, String> { "/graphql" } // unngå høykardinal uri
                    ).resolver(DefaultAddressResolverGroup.INSTANCE)
                )
            )
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()

    @Bean
    @Qualifier("pdlGraphQLClient")
    fun pdlGraphQLClient(
        @Qualifier("pdlWebClient") webClient: WebClient,
        @Value("\${PDL_URL}") pdlUrl: String
    ): GraphQLWebClient =
        GraphQLWebClient(
            url = pdlUrl,
            builder = webClient.mutate() // arver connector + observation
        )

    @Bean
    fun kodeverkWebClient(builder: WebClient.Builder,
                          @Qualifier("kodeverkObservation")
                          convention: ClientRequestObservationConvention,
                          navCallIdHeaderFilter: ExchangeFilterFunction): WebClient =
        builder
            .baseUrl(kodeverkURL)
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create().metrics(true,
                        java.util.function.Function<String, String> { uri ->
                            // Returnér hva du vil tagge som "uri" (f.eks. masker variabler)
                            uri
                        })
                )
            )
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()

    @Bean
    fun navCallIdHeaderFilter(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { req ->
            Mono.deferContextual { ctx ->
                val callId = ctx.getOrDefault(CallId.CTX_KEY, MDC.get(CallId.HEADER) ?: UUID.randomUUID().toString())
                val mutated = ClientRequest.from(req)
                    .header(CallId.HEADER, callId)
                    .build()
                Mono.just(mutated)
            }
        }
}
