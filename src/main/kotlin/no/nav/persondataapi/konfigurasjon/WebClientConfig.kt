package no.nav.persondataapi.konfigurasjon

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import io.micrometer.observation.ObservationRegistry
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.resolver.AddressResolverGroup
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
import reactor.netty.resources.ConnectionProvider
import java.net.InetSocketAddress
import java.time.Duration
import java.util.UUID

@Configuration
class WebClientConfig(private val observationRegistry: ObservationRegistry) {

    private data class HttpClientKonfig(
        val poolNavn: String,
        val connectTimeout: Duration = Duration.ofSeconds(5),
        val responseTimeout: Duration = Duration.ofSeconds(30),
        val readTimeout: Duration = Duration.ofSeconds(50),
        val resolver: AddressResolverGroup<InetSocketAddress>? = null,
        val uriTagger: (String) -> String = { it }
    )

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

    @Value("\${MODIA_CONTEXT_HOLDER_URL}")
    lateinit var modiaContextHolderUrl: String

    @Value("\${NORG2_URL}")
    lateinit var norg2URL: String


    /**
     * HttpClient for tokenutveksling med pool.
     */
    @Bean
    @Qualifier("tokenHttpClient")
    fun tokenHttpClient(): HttpClient = httpClientFor("token")

    @Bean
    fun tokenWebClient(
        builder: WebClient.Builder,
        @Qualifier("tokenHttpClient") tokenHttpClient: HttpClient
    ): WebClient =
        builder
            .baseUrl(tokenExchangeUrl)
            .defaultHeader("Content-Type", "application/json")
            .clientConnector(ReactorClientHttpConnector(tokenHttpClient))
            .build()


    /**
     * HttpClient for Azure token-endepunktet med pool.
     */
    @Bean
    @Qualifier("azureTokenHttpClient")
    fun azureTokenHttpClient(): HttpClient = httpClientFor("azure-token")


    @Bean
    fun azuretokenWebClient(
        builder: WebClient.Builder,
        @Qualifier("azureTokenHttpClient") azureTokenHttpClient: HttpClient
    ): WebClient =
        builder
            .baseUrl(azureTokenEndpointUrl)
            .defaultHeader("Content-Type", "application/json")
            .clientConnector(ReactorClientHttpConnector(azureTokenHttpClient))
            .build()


    /**
     * HttpClient for utbetaling med pool og tidsavbrudd.
     */
    @Bean
    @Qualifier("utbetalingHttpClient")
    fun utbetalingHttpClient(): HttpClient = httpClientFor("utbetaling")


    @Bean
    fun utbetalingWebClient(
        builder: WebClient.Builder,
        @Qualifier("utbetalingObservation")
        convention: ClientRequestObservationConvention,
        navCallIdHeaderFilter: ExchangeFilterFunction,
        @Qualifier("utbetalingHttpClient") utbetalingHttpClient: HttpClient
    ): WebClient {

        return builder
            .baseUrl("$utbetalingURL/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern")
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(ReactorClientHttpConnector(utbetalingHttpClient))
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()
    }

    /**
     * HttpClient for inntektsklienten med navngitt pool og keep-alive.
     */
    @Bean
    @Qualifier("inntektHttpClient")
    fun inntektHttpClient(): HttpClient = httpClientFor("inntekt")

    @Bean
    fun inntektWebClient(
        builder: WebClient.Builder,
        @Qualifier("inntektObservation")
        convention: ClientRequestObservationConvention,
        navCallIdHeaderFilter: ExchangeFilterFunction,
        @Qualifier("inntektHttpClient") inntektHttpClient: HttpClient
    ): WebClient =
        builder
            .baseUrl(inntektURL)
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(ReactorClientHttpConnector(inntektHttpClient))
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()


    /**
     * HttpClient for tilgangsmaskin med pool.
     */
    @Bean
    @Qualifier("tilgangHttpClient")
    fun tilgangHttpClient(): HttpClient = httpClientFor("tilgang")


    @Bean
    fun tilgangWebClient(
        builder: WebClient.Builder,
        navCallIdHeaderFilter: ExchangeFilterFunction,
        @Qualifier("tilgangHttpClient") tilgangHttpClient: HttpClient
    ): WebClient =
        builder
            .baseUrl(tilgangmaskinURL)
            //.defaultHeader("Content-Type", "application/json")
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(ReactorClientHttpConnector(tilgangHttpClient))
            .filter(navCallIdHeaderFilter)
            .build()


    /**
     * HttpClient for Aareg-klienten med pool.
     */
    @Bean
    @Qualifier("aaregHttpClient")
    fun aaregHttpClient(): HttpClient = httpClientFor("aareg")


    @Bean
    fun aaregWebClient(
        builder: WebClient.Builder,
        @Qualifier("aaregObservation")
        convention: ClientRequestObservationConvention,
        navCallIdHeaderFilter: ExchangeFilterFunction,
        @Qualifier("aaregHttpClient") aaregHttpClient: HttpClient
    ): WebClient =
        builder
            .baseUrl(aaregURL)
            //.defaultHeader("Content-Type", "application/json")
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(ReactorClientHttpConnector(aaregHttpClient))
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()

    /**
     * HttpClient for Ereg-klienten med pool.
     */
    @Bean
    @Qualifier("eregHttpClient")
    fun eregHttpClient(): HttpClient = httpClientFor("ereg")


    @Bean
    fun eregWebClient(
        builder: WebClient.Builder,
        navCallIdHeaderFilter: ExchangeFilterFunction,
        @Qualifier("eregHttpClient") eregHttpClient: HttpClient
    ): WebClient =
        builder
            .baseUrl(eregURL)
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(ReactorClientHttpConnector(eregHttpClient))
            .filter(navCallIdHeaderFilter)
            .build()


    /**
     * HttpClient for PDL-klienten med pool og resolver for DNS.
     */
    @Bean
    @Qualifier("pdlHttpClient")
    fun pdlHttpClient(): HttpClient = httpClientFor("pdl")

    @Bean
    @Qualifier("pdlWebClient")
    fun pdlWebClient(
        base: WebClient.Builder,
        @Qualifier("pdlObservation")
        convention: ClientRequestObservationConvention,
        navCallIdHeaderFilter: ExchangeFilterFunction,
        @Qualifier("pdlHttpClient") pdlHttpClient: HttpClient
    ): WebClient =
        base.clone()
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(ReactorClientHttpConnector(pdlHttpClient))
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

    /**
     * HttpClient for Kodeverk-klienten med pool.
     */
    @Bean
    @Qualifier("kodeverkHttpClient")
    fun kodeverkHttpClient(): HttpClient = httpClientFor("kodeverk")


    @Bean
    fun kodeverkWebClient(
        builder: WebClient.Builder,
        @Qualifier("kodeverkObservation")
        convention: ClientRequestObservationConvention,
        navCallIdHeaderFilter: ExchangeFilterFunction,
        @Qualifier("kodeverkHttpClient") kodeverkHttpClient: HttpClient
    ): WebClient =
        builder
            .baseUrl(kodeverkURL)
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(ReactorClientHttpConnector(kodeverkHttpClient))
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()

    /**
     * HttpClient for NORG2-klienten med pool.
     */
    @Bean
    @Qualifier("norg2HttpClient")
    fun norg2HttpClient(): HttpClient = httpClientFor("norg2")


    @Bean
    fun norg2WebClient(
        builder: WebClient.Builder,
        @Qualifier("norg2Observation")
        convention: ClientRequestObservationConvention,
        navCallIdHeaderFilter: ExchangeFilterFunction,
        @Qualifier("norg2HttpClient") norg2HttpClient: HttpClient
    ): WebClient =
        builder
            .baseUrl(norg2URL)
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(ReactorClientHttpConnector(norg2HttpClient))
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()

    /**
     * HttpClient for Modia Context Holder med pool.
     */
    @Bean
    @Qualifier("modiaContextHolderHttpClient")
    fun modiaContextHolderHttpClient(): HttpClient = httpClientFor("modia-context-holder")


    @Bean
    fun modiaContextHolderWebClient(
        builder: WebClient.Builder,
        @Qualifier("modiaContextHolderObservation")
        convention: ClientRequestObservationConvention,
        navCallIdHeaderFilter: ExchangeFilterFunction,
        @Qualifier("modiaContextHolderHttpClient") modiaContextHolderHttpClient: HttpClient
    ): WebClient =
        builder
            .baseUrl(modiaContextHolderUrl)
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .clientConnector(ReactorClientHttpConnector(modiaContextHolderHttpClient))
            .observationConvention(convention)
            .filter(navCallIdHeaderFilter)
            .build()

    @Bean
    fun navCallIdHeaderFilter(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { req ->
            Mono.deferContextual { ctx ->
                val callId: String = ctx.getOrDefault<String>(
                    CallId.CTX_KEY, null)
                    ?: (MDC.get(CallId.HEADER)
                    ?: UUID.randomUUID().toString()
                )
                val mutated = ClientRequest.from(req)
                    .header(CallId.HEADER, callId)
                    .build()
                Mono.just(mutated)
            }
        }

    private val httpClientKonfigurasjoner = mapOf(
        "utbetaling" to HttpClientKonfig(
            poolNavn = "utbetaling-pool",
            readTimeout = Duration.ofSeconds(10),
            responseTimeout = Duration.ofSeconds(10),
            ),
        "inntekt" to HttpClientKonfig(
            poolNavn = "inntekt-pool",
            readTimeout = Duration.ofSeconds(10),
            responseTimeout = Duration.ofSeconds(10),
        ),
        "aareg" to HttpClientKonfig(poolNavn = "aareg-pool"),
        "ereg" to HttpClientKonfig(poolNavn = "ereg-pool"),
        "pdl" to HttpClientKonfig(
            poolNavn = "pdl-pool",
            resolver = DefaultAddressResolverGroup.INSTANCE,
            uriTagger = { "/graphql" }
        ),
        "norg2" to HttpClientKonfig(poolNavn = "norg2-pool"),
        "tilgang" to HttpClientKonfig(poolNavn = "tilgang-pool"),
        "kodeverk" to HttpClientKonfig(poolNavn = "kodeverk-pool"),
        "modia-context-holder" to HttpClientKonfig(poolNavn = "modia-context-holder-pool"),
        "token" to HttpClientKonfig(poolNavn = "token-pool"),
        "azure-token" to HttpClientKonfig(poolNavn = "azure-token-pool")
    )

    private fun httpClientFor(navn: String): HttpClient = httpClientMedPool(httpClientKonfigurasjoner.getValue(navn))

    private fun httpClientMedPool(konfig: HttpClientKonfig): HttpClient {
        val connectionProvider = ConnectionProvider.builder(konfig.poolNavn)
            .maxConnections(30)
            // Brannmuren mellom FSS og GCP dropper idle connections etter 60 minutter
            .maxIdleTime(Duration.ofMinutes(55))
            .maxLifeTime(Duration.ofMinutes(59))
            .evictInBackground(Duration.ofMinutes(5))
            .build()

        val httpClient = HttpClient.create(connectionProvider)
            .keepAlive(true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, konfig.connectTimeout.toMillis().toInt())
            .responseTimeout(konfig.responseTimeout)
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(konfig.readTimeout.toSeconds().toInt()))
            }
            .metrics(true, java.util.function.Function<String, String> { uri -> konfig.uriTagger(uri) })

        return konfig.resolver?.let { httpClient.resolver(it) } ?: httpClient
    }
}
