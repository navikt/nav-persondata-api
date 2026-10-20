package no.nav.persondataapi.konfigurasjon

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.generated.pdl.HentAktorId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient

class PdlWebClientConfigTest {
    @Test
    fun `Permissive-Mode sendes kun i dev-gcp`() {
        val cases =
            listOf(
                MiljøTesttilfelle("dev-gcp", "true"),
                MiljøTesttilfelle("prod-gcp", null),
                MiljøTesttilfelle("local", null),
                MiljøTesttilfelle("ukjent", null),
                MiljøTesttilfelle(null, null),
            )

        cases.forEach { testtilfelle ->
            val wireMock = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMock.start()
            try {
                wireMock.stubFor(
                    WireMock
                        .post(WireMock.urlPathEqualTo("/graphql"))
                        .willReturn(
                            WireMock
                                .aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""{"data":{"hentIdenter":{"identer":[]}}}"""),
                        ),
                )

                val context = AnnotationConfigApplicationContext()
                context.environment.propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
                context.environment.propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)
                val properties =
                    mutableMapOf<String, Any>(
                        "PDL_URL" to "${wireMock.baseUrl()}/graphql",
                    ).apply {
                        testtilfelle.naisClusterName?.let { put("NAIS_CLUSTER_NAME", it) }
                    }
                context.environment.propertySources.addFirst(MapPropertySource("pdl-test", properties))
                context.register(PdlWebClientConfigTestConfiguration::class.java)
                context.refresh()
                try {
                    runBlocking {
                        context
                            .getBean(GraphQLWebClient::class.java)
                            .execute(HentAktorId(HentAktorId.Variables(ident = "12345678901")))
                    }

                    assertEquals(
                        testtilfelle.permissiveMode,
                        wireMock.allServeEvents
                            .single()
                            .request
                            .getHeader("Permissive-Mode"),
                        "NAIS_CLUSTER_NAME=${testtilfelle.naisClusterName ?: "(mangler)"}",
                    )
                } finally {
                    context.close()
                }
            } finally {
                wireMock.stop()
            }
        }
    }

    private data class MiljøTesttilfelle(
        val naisClusterName: String?,
        val permissiveMode: String?,
    )
}

@TestConfiguration(proxyBeanMethods = false)
class PdlWebClientConfigTestConfiguration {
    @Bean
    fun observationRegistry(): ObservationRegistry = ObservationRegistry.create()

    @Bean
    fun pdlWebClient(
        observationRegistry: ObservationRegistry,
        @Value("\${NAIS_CLUSTER_NAME:local}") naisClusterName: String,
    ): WebClient =
        WebClientConfig(observationRegistry, naisClusterName)
            .pdlWebClient(
                base = WebClient.builder(),
                convention = DefaultClientRequestObservationConvention(),
                navCallIdHeaderFilter = ExchangeFilterFunction.ofRequestProcessor { Mono.just(it) },
                pdlHttpClient = HttpClient.create(),
            )

    @Bean
    fun pdlGraphQLClient(
        webClient: WebClient,
        observationRegistry: ObservationRegistry,
        @Value("\${PDL_URL}") pdlUrl: String,
        @Value("\${NAIS_CLUSTER_NAME:local}") naisClusterName: String,
    ): GraphQLWebClient =
        WebClientConfig(observationRegistry, naisClusterName)
            .pdlGraphQLClient(webClient, pdlUrl)
}
