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
import org.springframework.web.bind.annotation.RequestMapping
import kotlin.time.measureTimedValue

@Controller
@RequestMapping("/oppslag/inntekt")
class InntektController(
    val inntektClient: InntektClient,
    val eregClient: EregClient,
    val brukertilgangService: BrukertilgangService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)
    @Protected
    @PostMapping
    fun hentInntekter(@RequestBody dto: OppslagRequestDto): ResponseEntity<OppslagResponseDto<InntektInformasjon>> {
        val anonymisertIdent = dto.ident.take(6) + "*****"
        return runBlocking {
            if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(dto.ident)) {
                logger.info("Saksbehandler har ikke tilgang til å hente inntekter for $anonymisertIdent")
                ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
            }
            val (inntektResponse, tid) = measureTimedValue {
                inntektClient.hentInntekter(dto.ident)
            }

            logger.info("Hentet inntekter for $anonymisertIdent på ${tid.inWholeMilliseconds} ms, status ${inntektResponse.statusCode}")

            when (inntektResponse.statusCode) {
                404 -> ResponseEntity(OppslagResponseDto(error = "Person ikke funnet", data = null), HttpStatus.NOT_FOUND)
                403 -> ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
                500 -> ResponseEntity(OppslagResponseDto(error = "Feil i baksystem", data = null), HttpStatus.BAD_GATEWAY)
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

            logger.info("Fant ${lønnsinntekt.size} lønnsinntekt(er) for $anonymisertIdent")

            ResponseEntity.ok(
                OppslagResponseDto(
                    data = InntektInformasjon(lønnsinntekt = lønnsinntekt)
                )
            )

        }
    }
}
