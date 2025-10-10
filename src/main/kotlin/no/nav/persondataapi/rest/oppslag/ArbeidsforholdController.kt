package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.aareg.client.AaregClient
import no.nav.persondataapi.aareg.client.hentIdenter
import no.nav.persondataapi.ereg.client.EregClient
import no.nav.persondataapi.ereg.client.EregRespons
import no.nav.persondataapi.rest.domain.ArbeidsgiverInformasjon
import no.nav.persondataapi.service.BrukertilgangService
import no.nav.persondataapi.service.mapArbeidsforholdTilArbeidsgiverData
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
            val anonymisertIdent = dto.ident.take(6) + "*****"
            if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(dto.ident)) {
                logger.info("Saksbehandler har ikke tilgang til å hente stønader for $anonymisertIdent")
                ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
            }
            val (aaregRespons, tid) = measureTimedValue {
                aaregClient.hentArbeidsforhold(dto.ident)
            }

            logger.info("Hentet arbeidsforhold for $anonymisertIdent på ${tid.inWholeMilliseconds} ms, status ${aaregRespons.statusCode}")

            when (aaregRespons.statusCode) {
                404 -> ResponseEntity(OppslagResponseDto(error = "Person ikke funnet", data = null), HttpStatus.NOT_FOUND)
                403 -> ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
                500 -> ResponseEntity(OppslagResponseDto(error = "Feil i baksystem", data = null), HttpStatus.BAD_GATEWAY)
            }

            val alleArbeidsforhold = aaregRespons.data

            if (alleArbeidsforhold.isEmpty()) {
                logger.info("Fant ingen arbeidsforhold for $anonymisertIdent")
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

            logger.info("Fant ${løpendeArbeidsforhold.size} løpende og ${historiskeArbeidsforhold.size} historiske arbeidsforhold for $anonymisertIdent")

            ResponseEntity.ok(
                OppslagResponseDto(
                    data = ArbeidsgiverInformasjon(
                        løpendeArbeidsforhold, historiskeArbeidsforhold
                    )
                )
            )

        }
    }
}
