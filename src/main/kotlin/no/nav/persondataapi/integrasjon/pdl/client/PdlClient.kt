package no.nav.persondataapi.integrasjon.pdl.client


import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.persondataapi.generated.HentGeografiskTilknytning
import no.nav.persondataapi.generated.HentPerson
import no.nav.persondataapi.generated.hentgeografisktilknytning.GeografiskTilknytning
import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class PdlClient(
    private val tokenService: TokenService,

    @param:Value("\${PDL_URL}")
    private val pdlUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    @Cacheable(value = ["pdl-person"], key = "#personIdent")
    suspend fun hentPerson(personIdent: PersonIdent): PersonDataResultat {
        val token = tokenService.getServiceToken(SCOPE.PDL_SCOPE)

        val client = GraphQLWebClient(
            url = pdlUrl,
            builder = WebClient.builder(),
        )
        val query = HentPerson(
            HentPerson.Variables(
                ident = personIdent.value,
                historikk = false,
            )
        )
        val response = client.execute(query) {
            header("Authorization", "Bearer $token")
            header(Behandlingsnummer, "B634")
            header(Tema, "KTR")
        }
        if (response.errors?.isNotEmpty() == true) {
            var status = 500
            if ("not_found".equals(response.errors!!.first().extensions?.get("code"))){
                status = 404
            }
            log.error("Feil i kall mot PDL : ${response.errors!!.first().message}")
            return PersonDataResultat(
                data = null,
                statusCode = status,
                errorMessage = response.errors?.get(0)?.message,
            )
        }
        return PersonDataResultat(
            data = response.data!!.hentPerson,
            statusCode = 200,
            errorMessage = null,
        )
    }

    @Cacheable(value = ["pdl-geografisktilknytning"], key = "#personIdent")
    suspend fun hentGeografiskTilknytning(personIdent: PersonIdent): GeografiskTilknytningResultat {
        val token = tokenService.getServiceToken(SCOPE.PDL_SCOPE)

        val client = GraphQLWebClient(
            url = pdlUrl,
            builder = WebClient.builder(),
        )
        val query = HentGeografiskTilknytning(
            HentGeografiskTilknytning.Variables(
                ident = personIdent.value,
            )
        )
        val response = client.execute(query) {
            header("Authorization", "Bearer $token")
            header(Behandlingsnummer, "B634")
            header(Tema, "KTR")
        }
        if (response.errors?.isNotEmpty() == true) {
            var status = 500
            if ("not_found".equals(response.errors!!.first().extensions?.get("code"))){
                status = 404
            }
            log.error("Feil i kall mot PDL : ${response.errors!!.first().message}")
            return GeografiskTilknytningResultat(
                data = null,
                statusCode = status,
                errorMessage = response.errors?.get(0)?.message,
            )
        }
        return GeografiskTilknytningResultat(
            data = response.data!!.hentGeografiskTilknytning,
            statusCode = 200,
            errorMessage = null,
        )
    }

    companion object CustomHeaders {
        const val Behandlingsnummer = "behandlingsnummer"
        const val Tema = "TEMA"
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
