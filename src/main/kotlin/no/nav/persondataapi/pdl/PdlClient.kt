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
    @Qualifier("pdlGraphQLClient")
    private val client: GraphQLWebClient,

    @Value("\${PDL_URL}")
    private val pdl_url: String,


) {

    suspend fun hentPersonv2(ident: String, userToken: String): PersonDataResultat {


        val oboToken = tokenService.exchangeToken(
            userToken, SCOPE.PDL_SCOPE
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
            var status = 500
            if ("not_found".equals(response.errors!!.first().extensions?.get("code"))){
              status = 404
            }
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
    suspend fun hentPersonv2(ident: String): PersonDataResultat {


        val token = tokenService.getServiceToken(SCOPE.PDL_SCOPE
        )

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
            header("Authorization", "Bearer $token")
            header(CustomHeaders.Behandlingsnummer, "B634")
            header(CustomHeaders.Tema, "KTR")
        }
        if (response.errors?.isNotEmpty() == true) {
            var status = 500
            if ("not_found".equals(response.errors!!.first().extensions?.get("code"))){
                status = 404
            }
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
}

