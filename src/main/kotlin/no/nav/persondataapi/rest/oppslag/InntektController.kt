package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
import no.nav.inntekt.generated.model.Loennsinntekt
import no.nav.persondataapi.ereg.client.EregClient
import no.nav.persondataapi.inntekt.client.InntektClient
import no.nav.persondataapi.rest.domain.InntektInformasjon
import no.nav.persondataapi.rest.domain.Lønnsdetaljer
import no.nav.persondataapi.service.BrukertilgangService
import no.nav.persondataapi.service.harHistorikkPåNormallønn
import no.nav.persondataapi.service.nyeste
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller("/oppslag")
class InntektController(
    val inntektClient: InntektClient,
    val eregClient: EregClient,
    val brukertilgangService: BrukertilgangService
) {
    @Protected
    @PostMapping("/inntekt")
    fun hentInntekter(@RequestBody dto: InntektRequestDto): ResponseEntity<InntektResponseDto> {
        return runBlocking {
            if (!brukertilgangService.harBrukerTilgangTilIdent(dto.ident)) {
                ResponseEntity(InntektResponseDto(error = "Ingen tilgang"), HttpStatus.FORBIDDEN)
            }
            val inntektResponse = inntektClient.hentInntekter(dto.ident)

            when (inntektResponse.statusCode) {
                404 -> ResponseEntity(InntektResponseDto(error = "Person ikke funnet"), HttpStatus.NOT_FOUND)
                403 -> ResponseEntity(InntektResponseDto(error = "Ingen tilgang"), HttpStatus.FORBIDDEN)
            }

            val lønnsinntekt = inntektResponse.data?.data
                .orEmpty()
                .flatMap { historikk ->
                    val arbeidsgiver = eregClient.hentOrganisasjon(historikk.opplysningspliktig)

                    historikk.versjoner.nyeste()
                        ?.inntektListe
                        ?.filterIsInstance<Loennsinntekt>()
                        ?.map { loenn ->
                            Lønnsdetaljer(
                                arbeidsgiver = arbeidsgiver.navn?.sammensattnavn,
                                periode = historikk.maaned,
                                arbeidsforhold = "",
                                stillingsprosent = "",
                                lønnstype = loenn.beskrivelse,
                                antall = loenn.antall,
                                beløp = loenn.beloep,
                                harFlereVersjoner = historikk.harHistorikkPåNormallønn()
                            )
                        }
                        .orEmpty()
                }

            ResponseEntity.ok(
                InntektResponseDto(
                    data = InntektInformasjon(lønnsinntekt = lønnsinntekt)
                )
            )

        }
    }
}

data class InntektRequestDto(val ident: String)
data class InntektResponseDto(
    val error: String? = null,
    val data: InntektInformasjon? = null,
)
