package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.rest.domene.PersonInformasjon
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period

@Service
class PersonopplysningerService(
    private val pdlClient: PdlClient,
    private val brukertilgangService: BrukertilgangService,
    private val kodeverkService: KodeverkService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentPersonopplysningerForPerson(personIdent: String): PersonopplysningerResultat {
        // Sjekk tilgang først
        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente personopplysninger for $personIdent")
            return PersonopplysningerResultat.IngenTilgang
        }

        // Hent person fra PDL
        val pdlResponse = pdlClient.hentPerson(personIdent)
        logger.info("Hentet personopplysninger for $personIdent, status ${pdlResponse.statusCode}")

        // Håndter feil fra PdlClient
        when (pdlResponse.statusCode) {
            404 -> return PersonopplysningerResultat.PersonIkkeFunnet
            403 -> return PersonopplysningerResultat.IngenTilgang
            500 -> return PersonopplysningerResultat.FeilIBaksystem
            !in 200..299 -> return PersonopplysningerResultat.FeilIBaksystem
        }

        val pdlData = pdlResponse.data ?: return PersonopplysningerResultat.PersonIkkeFunnet

        // Mappe familie og sivilstand
        val foreldreOgBarn = pdlData.forelderBarnRelasjon.associate {
            Pair(it.relatertPersonsIdent!!, it.relatertPersonsRolle.name)
        }
        val statsborgerskap = pdlData.statsborgerskap.map { it.land }
        val ektefelle = pdlData.sivilstand
            .filter { it.relatertVedSivilstand != null }
            .associate { Pair(it.relatertVedSivilstand!!, it.type.name) }
        val familiemedlemmer = foreldreOgBarn + ektefelle

        // Mappe personopplysninger
        val personopplysninger = PersonInformasjon(
            navn = PersonInformasjon.Navn(
                pdlData.gjeldendeFornavn(),
                mellomnavn = pdlData.gjeldendeMellomnavn(),
                etternavn = pdlData.gjeldendeEtternavn(),
            ),
            aktørId = personIdent,
            adresse = pdlData.nåværendeBostedsadresse(),
            familemedlemmer = familiemedlemmer,
            statsborgerskap = statsborgerskap,
            sivilstand = pdlData.gjeldendeSivilStand(),
            alder = pdlData.foedselsdato.first().foedselsdato?.let {
                Period.between(LocalDate.parse(it), LocalDate.now()).years
            } ?: -1,
        )

        // Berik med kodeverkdata
        val beriketPersonopplysninger = berikMedKodeverkData(personopplysninger)

        logger.info("Hentet og mappet personopplysninger for $personIdent")

        return PersonopplysningerResultat.Success(beriketPersonopplysninger)
    }

    private fun berikMedKodeverkData(input: PersonInformasjon): PersonInformasjon {
        return input.copy(
            statsborgerskap = input.statsborgerskap.map { kodeverkService.mapLandkodeTilLandnavn(it) },
            adresse = input.adresse?.let { adresse ->
                adresse.copy(
                    utenlandskAdresse = adresse.utenlandskAdresse?.copy(
                        landkode = kodeverkService.mapLandkodeTilLandnavn(adresse.utenlandskAdresse.landkode)
                    )
                )
            }
        )
    }
}

sealed class PersonopplysningerResultat {
    data class Success(val data: PersonInformasjon) : PersonopplysningerResultat()
    data object IngenTilgang : PersonopplysningerResultat()
    data object PersonIkkeFunnet : PersonopplysningerResultat()
    data object FeilIBaksystem : PersonopplysningerResultat()
}
