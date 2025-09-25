package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.pdl.client.PdlClient
import no.nav.persondataapi.rest.domain.Navn
import no.nav.persondataapi.rest.domain.PersonInformasjon
import no.nav.persondataapi.service.gjeldendeEtternavn
import no.nav.persondataapi.service.gjeldendeFornavn
import no.nav.persondataapi.service.gjeldendeMellomnavn
import no.nav.persondataapi.service.gjeldendeSivilStand
import no.nav.persondataapi.service.nåværendeBostedsadresse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.time.LocalDate
import java.time.Period


@Controller("/oppslag")
class PersonopplysningerController(val pdlClient: PdlClient) {
    @Protected
    @PostMapping("/personopplysninger")
    fun hentPersonopplysninger(@RequestBody dto: PersonopplysningerRequestDto): ResponseEntity<PersonopplysningerResponseDto> {
        return runBlocking {
            val resultat = pdlClient.hentPersonv2(dto.ident)

            if (resultat.statusCode == 404 || resultat.data == null) {
               ResponseEntity(PersonopplysningerResponseDto(error = "Person ikke funnet"),HttpStatus.NOT_FOUND)
            }

            if (resultat.statusCode == 403) {
               ResponseEntity(PersonopplysningerResponseDto(error = "Ingen tilgang"),HttpStatus.FORBIDDEN)
            }

            val pdlResultat  = resultat.data as Person
            val foreldreOgBarn = pdlResultat.forelderBarnRelasjon.associate { Pair(it.relatertPersonsIdent!!, it.relatertPersonsRolle.name) }
            val statsborgerskap = pdlResultat.statsborgerskap.map { it.land }
            val ektefelle = pdlResultat.sivilstand.filter { it.relatertVedSivilstand!=null }.associate { Pair(it.relatertVedSivilstand!!,it.type.name)}
            val familiemedlemmer = foreldreOgBarn + ektefelle

            val personopplysninger = PersonInformasjon(
                navn = Navn(
                    pdlResultat.gjeldendeFornavn(),
                    mellomnavn = pdlResultat.gjeldendeMellomnavn(),
                    etternavn = pdlResultat.gjeldendeEtternavn(),
                ),
                aktørId = dto.ident,
                adresse = pdlResultat.nåværendeBostedsadresse(),
                familemedlemmer = familiemedlemmer,
                statsborgerskap = statsborgerskap,
                sivilstand = pdlResultat.gjeldendeSivilStand(),
                alder = pdlResultat.foedselsdato.first().foedselsdato?.let { Period.between(LocalDate.parse(it), LocalDate.now()).years } ?: -1,
            )

            ResponseEntity(PersonopplysningerResponseDto(data = personopplysninger), HttpStatus.OK)
        }
    }
}

data class PersonopplysningerRequestDto(val ident: String)
data class PersonopplysningerResponseDto(
    val error: String? = null,
    val data: PersonInformasjon? = null
)
