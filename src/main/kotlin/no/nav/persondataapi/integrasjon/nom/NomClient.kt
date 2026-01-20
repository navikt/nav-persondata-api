package no.nav.persondataapi.integrasjon.nom

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import no.nav.persondataapi.generated.nom.RessursQuery
import no.nav.persondataapi.generated.nom.ressursquery.Ressurs
import no.nav.persondataapi.konfigurasjon.RetryPolicy.coroutineRetry
import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.metrics.NomMetrics
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Service
class NomClient(
    private val tokenService: TokenService,
    private val metrics: NomMetrics,

    @param:Value("\${NOM_URL}")
    private val nomUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)


    fun createTimeoutHttpClient(): HttpClient {
        return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(5))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(5))
                conn.addHandlerLast(WriteTimeoutHandler(5))
            }
    }

    @Cacheable(
        value = ["nom-ressurs"],
        key = "#navIdent",
        unless = "#result.statusCode != 200 && #result.statusCode != 404"
    )
    suspend fun hentRessurs(navIdent: String): RessursResultat {
        val token = tokenService.getServiceToken(SCOPE.NOM_SCOPE)

        val httpClient = createTimeoutHttpClient()

        val client = GraphQLWebClient(
            url = nomUrl,
            builder = WebClient.builder()
                .clientConnector(ReactorClientHttpConnector(httpClient))
        )
        val query = RessursQuery(
            RessursQuery.Variables(
                navIdent = navIdent,
            )
        )
        try {
            val response = coroutineRetry(kilde = "NOM-Ressurs") {
                client.execute(query) {
                    header("Authorization", "Bearer $token")
                }
            }

            val errors = response.errors.orEmpty()
            if (errors.isNotEmpty()) {
                val (status, message) = håndterNomFeil(errors)
                return RessursResultat(
                    data = null,
                    statusCode = status,
                    errorMessage = message
                )
            }

            return RessursResultat(
                data = response.data!!.ressurs,
                statusCode = 200,
                errorMessage = null,
            )
        } catch (e: Exception) {
            return handleNomException(e, "HentRessurs")
        }
    }

    internal fun håndterNomFeil(errors: List<GraphQLClientError>): Pair<Int, String?> {
        val firstError = errors.first()
        val code = firstError.extensions?.get("code") as? String

        val status = when (code) {
            "not_found" -> {
                log.info("Feil i kall mot NOM : ${firstError.message}")
                404
            }
            else -> {
                log.error("Feil i kall mot NOM : ${firstError.message}")
                500
            }
        }
        return status to firstError.message
    }

    private fun handleNomException(e: Exception, operasjon: String): RessursResultat {
        when (e) {
            is java.util.concurrent.TimeoutException,
            is io.netty.handler.timeout.ReadTimeoutException,
            is io.netty.handler.timeout.WriteTimeoutException -> {
                metrics.counter(operasjon, DownstreamResult.TIMEOUT).increment()
                log.error("Timeout mot NOM ($operasjon)", e)
                return RessursResultat(null, 504, "Timeout mot NOM")
            }

            else -> {
                metrics.counter(operasjon, DownstreamResult.UNEXPECTED).increment()
                log.error("Uventet feil mot NOM ($operasjon)", e)
                return RessursResultat(null, 500, e.message)
            }
        }
    }

}

data class RessursResultat(
    val data: Ressurs?,
    val statusCode: Int,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)
