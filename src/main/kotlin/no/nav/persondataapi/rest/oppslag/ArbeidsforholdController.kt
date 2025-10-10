package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.aareg.client.AaregClient
import no.nav.persondataapi.aareg.client.Arbeidsforhold
import no.nav.persondataapi.aareg.client.hentIdenter
import no.nav.persondataapi.ereg.client.EregClient
import no.nav.persondataapi.ereg.client.EregRespons
import no.nav.persondataapi.rest.domain.ArbeidsgiverInformasjon
import no.nav.persondataapi.service.BrukertilgangService
import no.nav.persondataapi.service.hentOrgNummerTilArbeidssted
import no.nav.persondataapi.service.orgNummerTilOrgNavn
import no.nav.persondataapi.service.orgnummerTilAdresse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import kotlin.time.measureTimedValue

@Controller
@RequestMapping("/oppslag/arbeidsforhold")
class ArbeidsforholdController(val aaregClient: AaregClient, val eregClient: EregClient, val brukertilgangService: BrukertilgangService) {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)
    @Protected
    @PostMapping
    fun hentArbeidsforhold(@RequestBody dto: OppslagRequestDto): ResponseEntity<OppslagResponseDto<ArbeidsgiverInformasjon>> {
        return runBlocking {
            if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(dto.ident.value)) {
                logger.info("Saksbehandler har ikke tilgang til å hente arbeidsforhold for ${dto.ident}")
                ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
            }
            val (aaregRespons, tid) = measureTimedValue {
                aaregClient.hentArbeidsforhold(dto.ident.value)
            }

            logger.info("Hentet arbeidsforhold for ${dto.ident} på ${tid.inWholeMilliseconds} ms, status ${aaregRespons.statusCode}")

            when (aaregRespons.statusCode) {
                404 -> ResponseEntity(OppslagResponseDto(error = "Person ikke funnet", data = null), HttpStatus.NOT_FOUND)
                403 -> ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
                500 -> ResponseEntity(OppslagResponseDto(error = "Feil i baksystem", data = null), HttpStatus.BAD_GATEWAY)
            }

            val alleArbeidsforhold = aaregRespons.data

            if (alleArbeidsforhold.isEmpty()) {
                logger.info("Fant ingen arbeidsforhold for ${dto.ident}")
                ResponseEntity.ok(
                    OppslagResponseDto(
                        data = ArbeidsgiverInformasjon(
                            løpendeArbeidsforhold = emptyList(), historikk = emptyList()
                        )
                    )
                )
            }

            val unikeOrganisasjonsnumre: Map<String, EregRespons> = alleArbeidsforhold
                .hentIdenter()
                .map { it.ident }
                .distinct()
                .associateWith { ident -> eregClient.hentOrganisasjon(ident) }


            val løpendeArbeidsforhold = alleArbeidsforhold.filter { it.ansettelsesperiode.sluttdato == null }.map { arbeidsforhold ->
                mapArbeidsforholdTilArbeidsgiverData(arbeidsforhold, unikeOrganisasjonsnumre)
            }
            val historiskeArbeidsforhold = alleArbeidsforhold.filter { it.ansettelsesperiode.sluttdato != null }.map { arbeidsforhold ->
                mapArbeidsforholdTilArbeidsgiverData(arbeidsforhold, unikeOrganisasjonsnumre)
            }

            logger.info("Fant ${løpendeArbeidsforhold.size} løpende og ${historiskeArbeidsforhold.size} historiske arbeidsforhold for ${dto.ident}")

            ResponseEntity.ok(
                OppslagResponseDto(
                    data = ArbeidsgiverInformasjon(
                        løpendeArbeidsforhold, historiskeArbeidsforhold
                    )
                )
            )

        }
    }

    private fun mapArbeidsforholdTilArbeidsgiverData(
        arbeidsforhold: Arbeidsforhold,
        eregDataRespons: Map<String, EregRespons>
    ): ArbeidsgiverInformasjon.ArbeidsgiverData {
        val orgnummer = arbeidsforhold.hentOrgNummerTilArbeidssted()
        return ArbeidsgiverInformasjon.ArbeidsgiverData(
            eregDataRespons.orgNummerTilOrgNavn(orgnummer),
            orgnummer,
            eregDataRespons.orgnummerTilAdresse(orgnummer),
            ansettelsesDetaljer = arbeidsforhold.ansettelsesdetaljer.map
            { ansettelsesdetaljer ->
                ArbeidsgiverInformasjon.AnsettelsesDetalj(
                    ansettelsesdetaljer.type,
                    ansettelsesdetaljer.avtaltStillingsprosent,
                    ansettelsesdetaljer.antallTimerPrUke,
                    ArbeidsgiverInformasjon.ÅpenPeriode(
                        ansettelsesdetaljer.rapporteringsmaaneder.fra,
                        ansettelsesdetaljer.rapporteringsmaaneder.til
                    ),
                    ansettelsesdetaljer.yrke.beskrivelse
                )
            },
        )
    }
}
