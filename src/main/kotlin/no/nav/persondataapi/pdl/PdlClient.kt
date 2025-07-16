package no.nav.persondataapi.pdl



import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.persondataapi.generated.HentPerson

import no.nav.persondataapi.common.extensions.CustomHeaders
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value


import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient





@Service
class PdlClient(
    @Qualifier("utbetalingWebClient")
    private val webClient: WebClient,
    @Value("\${PDL_URL}")
    private val pdl_url: String,


) {



    suspend fun hentPerson(ident: String, token: String): HentPerson.Result? {



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
            // Du kan evt. logge eller kaste exception her
            throw RuntimeException("GraphQL error: ${response.errors}")
        }

        return response.data
    }
}

