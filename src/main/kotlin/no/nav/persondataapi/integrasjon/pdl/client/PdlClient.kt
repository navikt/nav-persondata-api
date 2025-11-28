package no.nav.persondataapi.integrasjon.pdl.client


import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import no.nav.persondataapi.generated.HentGeografiskTilknytning
import no.nav.persondataapi.generated.HentPerson
import no.nav.persondataapi.generated.hentgeografisktilknytning.GeografiskTilknytning
import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.metrics.PdlMetrics
import no.nav.persondataapi.rest.domene.PersonIdent
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
class PdlClient(
    private val tokenService: TokenService,
    private val metrics: PdlMetrics,

    @param:Value("\${PDL_URL}")
    private val pdlUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)


    fun createTimeoutHttpClient(): HttpClient {
        return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(30))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(30))
                conn.addHandlerLast(WriteTimeoutHandler(30))
            }
    }

    @Cacheable(
        value = ["pdl-person"],
        key = "#personIdent",
        unless = "#result.statusCode != 200 && #result.statusCode != 404"
    )
    suspend fun hentPerson(personIdent: PersonIdent): PersonDataResultat {
        val token = tokenService.getServiceToken(SCOPE.PDL_SCOPE)

        val httpClient = createTimeoutHttpClient()

        val client = GraphQLWebClient(
            url = pdlUrl,
            builder = WebClient.builder()
                .clientConnector(ReactorClientHttpConnector(httpClient))
        )
        val query = HentPerson(
            HentPerson.Variables(
                ident = personIdent.value,
                historikk = false,
            )
        )
        try {

            val response = client.execute(query) {
                header("Authorization", "Bearer $token")
                header(Behandlingsnummer, "B634")
                header(Tema, "KTR")
            }

            val errors = response.errors.orEmpty()
            if (errors.isNotEmpty()) {
                val (status, message) = håndterPdlFeil(errors)
                return PersonDataResultat(
                    data = null,
                    statusCode = status,
                    errorMessage = message
                )
            }

            return PersonDataResultat(
                data = response.data!!.hentPerson,
                statusCode = 200,
                errorMessage = null,
            )
        } catch (e: Exception) {
            return handlePdlException(e, "HentPerson")
        }
    }

    @Cacheable(
        value = ["pdl-geografisktilknytning"],
        key = "#personIdent",
        unless = "#result.statusCode != 200 && #result.statusCode != 404"
    )
    suspend fun hentGeografiskTilknytning(personIdent: PersonIdent): GeografiskTilknytningResultat {
        val token = tokenService.getServiceToken(SCOPE.PDL_SCOPE)

        val httpClient = createTimeoutHttpClient()

        val client = GraphQLWebClient(
            url = pdlUrl,
            builder = WebClient.builder()
                .clientConnector(ReactorClientHttpConnector(httpClient))
        )
        val query = HentGeografiskTilknytning(
            HentGeografiskTilknytning.Variables(
                ident = personIdent.value,
            )
        )
        try {
            val response = client.execute(query) {
                header("Authorization", "Bearer $token")
                header(Behandlingsnummer, "B634")
                header(Tema, "KTR")
            }

            val errors = response.errors.orEmpty()
            if (errors.isNotEmpty()) {
                val (status, message) = håndterPdlFeil(errors)
                return GeografiskTilknytningResultat(
                    data = null,
                    statusCode = status,
                    errorMessage = message
                )
            }

            return GeografiskTilknytningResultat(
                data = response.data!!.hentGeografiskTilknytning,
                statusCode = 200,
                errorMessage = null,
            )
        } catch (ex: Exception) {
            return handlePdlExceptionGeo(ex, "HentGeografiskTilknytning")
        }

    }

    companion object CustomHeaders {
        const val Behandlingsnummer = "behandlingsnummer"
        const val Tema = "TEMA"
    }

    internal fun håndterPdlFeil(errors: List<GraphQLClientError>): Pair<Int, String?> {
        val firstError = errors.first()
        val code = firstError.extensions?.get("code") as? String

        val status = when (code) {
            "not_found" -> {
                log.info("Feil i kall mot PDL : ${firstError.message}")
                404
            }
            else -> {
                log.error("Feil i kall mot PDL : ${firstError.message}")
                500
            }
        }
        return status to firstError.message
    }

    private fun handlePdlException(e: Exception, opperasjon: String): PersonDataResultat {
        when (e) {
            is java.util.concurrent.TimeoutException,
            is io.netty.handler.timeout.ReadTimeoutException,
            is io.netty.handler.timeout.WriteTimeoutException -> {
                metrics.counter(opperasjon, DownstreamResult.TIMEOUT).increment()
                log.error("Timeout mot PDL ($opperasjon)", e)
                return PersonDataResultat(null, 504, "Timeout mot PDL")
            }

            else -> {
                metrics.counter(opperasjon, DownstreamResult.UNEXPECTED).increment()
                log.error("Uventet feil mot PDL ($opperasjon)", e)
                return PersonDataResultat(null, 500, e.message)
            }
        }
    }

    private fun handlePdlExceptionGeo(e: Exception, opperasjon: String): GeografiskTilknytningResultat {
        when (e) {
            is java.util.concurrent.TimeoutException,
            is io.netty.handler.timeout.ReadTimeoutException,
            is io.netty.handler.timeout.WriteTimeoutException -> {
                metrics.counter(opperasjon, DownstreamResult.TIMEOUT).increment()
                log.error("Timeout mot PDL ($opperasjon)", e)
                return GeografiskTilknytningResultat(null, 504, "Timeout mot PDL")
            }

            else -> {
                metrics.counter(opperasjon, DownstreamResult.UNEXPECTED).increment()
                log.error("Uventet feil mot PDL ($opperasjon)", e)
                return GeografiskTilknytningResultat(null, 500, e.message)
            }
        }
    }
}

data class PersonDataResultat(
    val data: Person?,
    val statusCode: Int,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)

data class GeografiskTilknytningResultat(
    val data: GeografiskTilknytning?,
    val statusCode: Int,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)

