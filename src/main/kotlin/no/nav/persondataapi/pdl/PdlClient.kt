package no.nav.persondataapi.pdl



import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.persondataapi.generated.HentPerson
import org.springframework.beans.factory.annotation.Qualifier

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient


@Service
class PdlClient(
    @Qualifier("utbetalingWebClient")
    private val webClient: WebClient,

) {



    suspend fun hentPerson(ident: String, token: String): HentPerson.Result? {



         val client = GraphQLWebClient(
             url = "https://pdl-api.dev-fss-pub.nais.io/graphql",
             builder = WebClient.builder(),
         )
        val query = HentPerson(
            HentPerson.Variables(
                ident = ident,
                bostedHistorikk = false,
                statsborgerskapHistorikk = false
            )
        )
        val response = client.execute(query) {
            header("Authorization", "Bearer $token")
        }
        if (response.errors?.isNotEmpty() == true) {
            // Du kan evt. logge eller kaste exception her
            throw RuntimeException("GraphQL error: ${response.errors}")
        }

        return response.data
    }
}

