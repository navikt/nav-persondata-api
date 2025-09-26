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

@Controller("/oppslag")
class ArbeidsforholdController(val aaregClient: AaregClient, val eregClient: EregClient, val brukertilgangService: BrukertilgangService) {
    @Protected
    @PostMapping("/arbeidsforhold")
    fun hentArbeidsforhold(@RequestBody dto: OppslagRequestDto): ResponseEntity<OppslagResponseDto<ArbeidsgiverInformasjon>> {
        return runBlocking {
            if (!brukertilgangService.harBrukerTilgangTilIdent(dto.ident)) {
                ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
            }
            val aaregRespons = aaregClient.hentArbeidsforhold(dto.ident)

            when (aaregRespons.statusCode) {
                404 -> ResponseEntity(OppslagResponseDto(error = "Person ikke funnet", data = null), HttpStatus.NOT_FOUND)
                403 -> ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
                500 -> ResponseEntity(OppslagResponseDto(error = "Feil i baksystem", data = null), HttpStatus.BAD_GATEWAY)
            }

            val alleArbeidsforhold = aaregRespons.data

            if (alleArbeidsforhold.isEmpty()) {
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
