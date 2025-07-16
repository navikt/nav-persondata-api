package no.nav.persondataapi.pdl



import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.persondataapi.generated.HentPerson

import no.nav.persondataapi.common.extensions.CustomHeaders
import no.nav.persondataapi.domain.PersonDataResultat
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value


import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient





@Service
class PdlClient(
    private val tokenService: TokenService,
    @Qualifier("utbetalingWebClient")
    private val webClient: WebClient,
    @Value("\${PDL_URL}")
    private val pdl_url: String,


) {

    suspend fun hentPerson(ident: String, oboToken: String): HentPerson.Result? {

         val client = GraphQLWebClient(
             url = pdl_url,
             builder = WebClient.builder(),
         )
        val query = HentPerson(
            HentPerson.Variables(
                ident = ident,
                historikk = false,
            )
        )
        val response = client.execute(query) {
            header("Authorization", "Bearer $oboToken")
            header(CustomHeaders.Behandlingsnummer, "B634")
            header(CustomHeaders.Tema, "KTR")
        }
        if (response.errors?.isNotEmpty() == true) {
            // Du kan evt. logge eller kaste exception her
            throw RuntimeException("GraphQL error: ${response.errors}")
        }

        return response.data
    }
    suspend fun hentPersonv2(ident: String, userToken: String): PersonDataResultat {


        val oboToken = tokenService.exchangeToken(
            userToken, SCOPE.PDL_SCOPE
        )
        println("OBO TOKEN RECEIVED -> $oboToken")


        val client = GraphQLWebClient(
            url = pdl_url,
            builder = WebClient.builder(),
        )
        val query = HentPerson(
            HentPerson.Variables(
                ident = ident,
                historikk = false,
            )
        )
        val response = client.execute(query) {
            header("Authorization", "Bearer $oboToken")
            header(CustomHeaders.Behandlingsnummer, "B634")
            header(CustomHeaders.Tema, "KTR")
        }
        if (response.errors?.isNotEmpty() == true) {
            return PersonDataResultat(
                data = null,
                statusCode = 500,
                errorMessage = response.errors?.get(0)?.message,
            )
            // Du kan evt. logge eller kaste exception her

        }
        return PersonDataResultat(
            data = response.data!!.hentPerson,
            statusCode = 200,
            errorMessage = null,
        )
    }
}

