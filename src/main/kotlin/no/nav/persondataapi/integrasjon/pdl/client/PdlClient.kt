package no.nav.persondataapi.integrasjon.pdl.client

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import no.nav.persondataapi.generated.pdl.HentGeografiskTilknytning
import no.nav.persondataapi.generated.pdl.HentPerson
import no.nav.persondataapi.generated.pdl.HentPersonBolk
import no.nav.persondataapi.generated.pdl.hentgeografisktilknytning.GeografiskTilknytning
import no.nav.persondataapi.generated.pdl.hentperson.Person
import no.nav.persondataapi.generated.pdl.hentpersonbolk.HentPersonBolkResult
import no.nav.persondataapi.konfigurasjon.RetryPolicy.coroutineRetry
import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.metrics.PdlMetrics
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class PdlClient(
    private val tokenService: TokenService,
    private val metrics: PdlMetrics,
    @param:Qualifier("pdlGraphQLClient")
    private val graphQLClient: GraphQLWebClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Cacheable(
        value = ["pdl-person"],
        key = "#personIdent",
        unless = "#result.statusCode != 200 && #result.statusCode != 404",
    )
    suspend fun hentPerson(personIdent: PersonIdent): PersonDataResultat {
        val token = tokenService.getServiceToken(SCOPE.PDL_SCOPE)

        val query =
            HentPerson(
                HentPerson.Variables(
                    ident = personIdent.value,
                    historikk = false,
                ),
            )
        try {
            val response =
                coroutineRetry(kilde = "PDL-HentPerson") {
                    graphQLClient.execute(query) {
                        header("Authorization", "Bearer $token")
                        header(BEHANDLINGSNUMMER, BEHANDLINGSNUMMER_VERDI)
                        header(TEMA, TEMA_VERDI)
                    }
                }

            val errors = response.errors.orEmpty()
            if (errors.isNotEmpty()) {
                val (status, message) = håndterPdlFeil(errors)
                return PersonDataResultat(
                    data = null,
                    statusCode = status,
                    errorMessage = message,
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
        unless = "#result.statusCode != 200 && #result.statusCode != 404",
    )
    suspend fun hentGeografiskTilknytning(personIdent: PersonIdent): GeografiskTilknytningResultat {
        val token = tokenService.getServiceToken(SCOPE.PDL_SCOPE)

        val query =
            HentGeografiskTilknytning(
                HentGeografiskTilknytning.Variables(
                    ident = personIdent.value,
                ),
            )
        try {
            val response =
                coroutineRetry(kilde = "PDL-Geografisktilknytning") {
                    graphQLClient.execute(query) {
                        header("Authorization", "Bearer $token")
                        header(BEHANDLINGSNUMMER, BEHANDLINGSNUMMER_VERDI)
                        header(TEMA, TEMA_VERDI)
                    }
                }

            val errors = response.errors.orEmpty()
            if (errors.isNotEmpty()) {
                val (status, message) = håndterPdlFeil(errors)
                return GeografiskTilknytningResultat(
                    data = null,
                    statusCode = status,
                    errorMessage = message,
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

    suspend fun hentPersonBolk(personIdenter: List<PersonIdent>): PersonBolkResultat {
        val token = tokenService.getServiceToken(SCOPE.PDL_SCOPE)
        val query =
            HentPersonBolk(
                HentPersonBolk.Variables(
                    identer = personIdenter.map { it.value },
                ),
            )
        return try {
            val response =
                coroutineRetry(kilde = "PDL-HentPersonBolk") {
                    graphQLClient.execute(query) {
                        header("Authorization", "Bearer $token")
                        header(BEHANDLINGSNUMMER, BEHANDLINGSNUMMER_VERDI)
                        header(TEMA, TEMA_VERDI)
                    }
                }

            val errors = response.errors.orEmpty()
            if (errors.isNotEmpty()) {
                val (status, message) = håndterPdlFeil(errors)
                return PersonBolkResultat(data = emptyList(), statusCode = status, errorMessage = message)
            }

            val hentPersonBolk =
                response.data?.hentPersonBolk
                    ?: return PersonBolkResultat(
                        data = emptyList(),
                        statusCode = 502,
                        errorMessage = "PDL returnerte tom data for hentPersonBolk",
                    )
            PersonBolkResultat(data = hentPersonBolk, statusCode = 200)
        } catch (e: Exception) {
            when (e) {
                is java.util.concurrent.TimeoutException,
                is io.netty.handler.timeout.ReadTimeoutException,
                is io.netty.handler.timeout.WriteTimeoutException,
                -> {
                    metrics.counter("HentPersonBolk", DownstreamResult.TIMEOUT).increment()
                    log.error("Timeout mot PDL (HentPersonBolk)", e)
                    PersonBolkResultat(data = emptyList(), statusCode = 504, errorMessage = TIMEOUT_MOT_PDL)
                }

                else -> {
                    metrics.counter("HentPersonBolk", DownstreamResult.UNEXPECTED).increment()
                    log.error("Uventet feil mot PDL (HentPersonBolk)", e)
                    PersonBolkResultat(data = emptyList(), statusCode = 500, errorMessage = e.message)
                }
            }
        }
    }

    companion object CustomHeaders {
        const val BEHANDLINGSNUMMER = "behandlingsnummer"
        const val TEMA = "TEMA"
        const val BEHANDLINGSNUMMER_VERDI = "B634"
        const val TEMA_VERDI = "KTR"
        const val TIMEOUT_MOT_PDL = "Timeout mot PDL"
    }

    internal fun håndterPdlFeil(errors: List<GraphQLClientError>): Pair<Int, String?> {
        val firstError = errors.first()
        val code = firstError.extensions?.get("code") as? String

        val status =
            when (code) {
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

    private fun handlePdlException(
        e: Exception,
        opperasjon: String,
    ): PersonDataResultat {
        when (e) {
            is java.util.concurrent.TimeoutException,
            is io.netty.handler.timeout.ReadTimeoutException,
            is io.netty.handler.timeout.WriteTimeoutException,
            -> {
                metrics.counter(opperasjon, DownstreamResult.TIMEOUT).increment()
                log.error("Timeout mot PDL ($opperasjon)", e)
                return PersonDataResultat(null, 504, TIMEOUT_MOT_PDL)
            }

            else -> {
                metrics.counter(opperasjon, DownstreamResult.UNEXPECTED).increment()
                log.error("Uventet feil mot PDL ($opperasjon)", e)
                return PersonDataResultat(null, 500, e.message)
            }
        }
    }

    private fun handlePdlExceptionGeo(
        e: Exception,
        opperasjon: String,
    ): GeografiskTilknytningResultat {
        when (e) {
            is java.util.concurrent.TimeoutException,
            is io.netty.handler.timeout.ReadTimeoutException,
            is io.netty.handler.timeout.WriteTimeoutException,
            -> {
                metrics.counter(opperasjon, DownstreamResult.TIMEOUT).increment()
                log.error("Timeout mot PDL ($opperasjon)", e)
                return GeografiskTilknytningResultat(null, 504, TIMEOUT_MOT_PDL)
            }

            else -> {
                metrics.counter(opperasjon, DownstreamResult.UNEXPECTED).increment()
                log.error("Uventet feil mot PDL ($opperasjon)", e)
                return GeografiskTilknytningResultat(null, 500, e.message)
            }
        }
    }
}

data class PersonBolkResultat(
    val data: List<HentPersonBolkResult>,
    val statusCode: Int,
    val errorMessage: String? = null,
)

data class PersonDataResultat(
    val data: Person?,
    val statusCode: Int, // f.eks. 200, 401, 500
    val errorMessage: String? = null,
)

data class GeografiskTilknytningResultat(
    val data: GeografiskTilknytning?,
    val statusCode: Int, // f.eks. 200, 401, 500
    val errorMessage: String? = null,
)
