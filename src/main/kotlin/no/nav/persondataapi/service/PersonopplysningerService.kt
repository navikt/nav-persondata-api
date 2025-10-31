package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.domene.PersonInformasjon
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period

@Service
class PersonopplysningerService(
    private val pdlClient: PdlClient,
    private val brukertilgangService: BrukertilgangService,
    private val kodeverkService: KodeverkService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val teamLogsMarker = MarkerFactory.getMarker("TEAM_LOGS")


    suspend fun finnesPerson(personIdent: PersonIdent): Boolean {
        val response = pdlClient.hentPerson(personIdent)
        return response.statusCode != 404
    }

    suspend fun hentPersonopplysningerForPerson(
        personIdent: PersonIdent,
        responsLog: Boolean = false
    ): PersonopplysningerResultat {

        // Hent person fra PDL
        val pdlResponse = pdlClient.hentPerson(personIdent)
        if (responsLog) {
            logger.info(teamLogsMarker,"Logging aktivert - full PDL-respons for {}: {}", personIdent, pdlResponse)
        }
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
            aktørId = personIdent.value,
            adresse = pdlData.nåværendeBostedsadresse(),
            familemedlemmer = familiemedlemmer,
            statsborgerskap = statsborgerskap,
            sivilstand = pdlData.gjeldendeSivilStand(),
            alder = pdlData.foedselsdato.first().foedselsdato?.let {
                Period.between(LocalDate.parse(it), LocalDate.now()).years
            } ?: -1,
            fødselsdato = pdlData.foedselsdato.first().foedselsdato ?: "",
            dødsdato = pdlData.doedsfall.firstOrNull()?.doedsdato,
        )

        // Berik med kodeverkdata
        var beriketPersonopplysninger = berikMedKodeverkData(personopplysninger)

        logger.info("Hentet og mappet personopplysninger for $personIdent")

        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente personopplysninger for $personIdent. Maskerer responsen")
            beriketPersonopplysninger = maskerObjekt(beriketPersonopplysninger)
        }

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
