package no.nav.persondataapi.integrasjon.inntekt.client

import no.nav.inntekt.generated.model.InntektshistorikkApiInn
import no.nav.inntekt.generated.model.InntektshistorikkApiUt
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID


@Component
class InntektClient(
    private val tokenService: TokenService,
    @param:Qualifier("inntektWebClient")
    private val webClient: WebClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)


    @Cacheable(
        value = ["inntekt-historikk"],
        key = "#personIdent + '_' + #kontrollPeriode.fom + '_' + #kontrollPeriode.tom",
        unless = "#result.statusCode != 200"
    )
    fun hentInntekter(
        personIdent: PersonIdent,
        kontrollPeriode: KontrollPeriode = KontrollPeriode(
            LocalDate.now().minusYears(5),
            LocalDate.now()
        )
    ): InntektDataResultat {
        return runCatching {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM")

            val requestBody = InntektshistorikkApiInn(
                personident = personIdent.value,
                filter = "NAVKontrollA-Inntekt",
                formaal = "NAVKontroll",
                maanedFom = kontrollPeriode.fom.format(formatter),
                maanedTom = kontrollPeriode.tom.format(formatter),
            )
            val oboToken = tokenService.getServiceToken(SCOPE.INNTEKT_SCOPE)


            val responseResult = webClient.post()
                .uri("/rest/v2/inntektshistorikk")
                .header("Authorization", "Bearer $oboToken")
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .bodyValue(requestBody)
                .exchangeToMono { response ->
                    val status = response.statusCode()
                    if (status.is2xxSuccessful) {
                        response.bodyToMono(object : ParameterizedTypeReference<InntektshistorikkApiUt>() {})
                    } else {
                        response.bodyToMono(String::class.java).map { body ->
                            throw RuntimeException("Feil fra inntektsAPI: HTTP $status – $body")
                        }
                    }
                }.block()!!
            responseResult
        }.fold(
            onSuccess = { inntekt ->
                InntektDataResultat(
                    data = inntekt,
                    statusCode = 200,
                    ""
                )
            },
            onFailure = { error ->
                log.error("Feil ved henting av inntekter : ${error.message}", error)
                InntektDataResultat(
                    data = null,
                    statusCode = 500,
                    errorMessage = "Feil ved lesing: ${error.message}"
                )
            }
        )
    }
}

data class InntektDataResultat(
    val data: InntektshistorikkApiUt?,
    val statusCode: Int?,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)

data class KontrollPeriode(
    val fom: LocalDate, val tom: LocalDate
)
