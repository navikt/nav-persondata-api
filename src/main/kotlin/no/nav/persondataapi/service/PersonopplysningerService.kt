package no.nav.persondataapi.service

import no.nav.persondataapi.generated.enums.AdressebeskyttelseGradering
import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.konfigurasjon.teamLogsMarker
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
    private val navTilhørighetService: NavTilhørighetService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

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
        val lokalKontor = navTilhørighetService.finnLokalKontorForPersonIdent(personIdent)
        if (responsLog) {
            logger.info(teamLogsMarker,"Logging aktivert - full PDL-respons for {}: {}", personIdent, JsonUtils.toJson(pdlResponse).toPrettyString())
            logger.info(teamLogsMarker,"Logging aktivert - full PDL-geografisk-Tilknytning respons for {}: {}", personIdent, JsonUtils.toJson(lokalKontor).toPrettyString())
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
            Pair(it.relatertPersonsIdent ?: "Ukjent", it.relatertPersonsRolle?.name ?: "Ukjent")
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
            adresseBeskyttelse = pdlData.nåværendeAdresseBeskyttelse(),
            statsborgerskap = statsborgerskap,
            sivilstand = pdlData.gjeldendeSivilStand(),
            alder = pdlData.foedselsdato.firstOrNull()?.foedselsdato?.let {
                Period.between(LocalDate.parse(it), LocalDate.now()).years
            } ?: -1,
            fødselsdato = pdlData.foedselsdato.firstOrNull()?.foedselsdato ?: "",
            dødsdato = pdlData.doedsfall.firstOrNull()?.doedsdato,
            navKontor = PersonInformasjon.NavKontor(
                enhetId = lokalKontor.enhetId,
                navn = lokalKontor.navn,
                enhetNr = lokalKontor.enhetNr,
                type = lokalKontor.type
            ),
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
                    norskAdresse = adresse.norskAdresse?.copy(
                        poststed = kodeverkService.mapPostnummerTilPoststed(adresse.norskAdresse.postnummer)
                    ),
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

/**
 * Henter personens nåværende adressebeskyttelse, basert på siste ikke-historiske oppføring.
 *
 * Denne funksjonen tolker adressebeskyttelsen for en `Person` slik:
 *  - Dersom listen `adressebeskyttelse` er tom, antas personen å ha **ingen skjerming**.
 *  - Første element i listen som **ikke er historisk** (`metadata.historisk == false`)
 *    brukes som gjeldende beskyttelse.
 *  - Graderingen mappes til verdier i `PersonInformasjon.Skjerming`:
 *      - `UGRADERT` → `UGRADERT`
 *      - `FORTROLIG` → `FORTROLIG`
 *      - `STRENGT_FORTROLIG` → `STRENGT_FORTROLIG`
 *      - `STRENGT_FORTROLIG_UTLAND` → `STRENGT_FORTROLIG_UTLAND`
 *      - Ukjente verdier (`__UNKNOWN_VALUE`) → `UGRADERT`
 *
 * Dersom ingen gyldig adressebeskyttelse finnes, returneres `UGRADERT ` som standard.
 *
 * @receiver `Person`-objektet som inneholder adressebeskyttelsesdata.
 * @return En verdi av typen [PersonInformasjon.Skjerming] som representerer gjeldende beskyttelsesnivå.
 */
fun Person.nåværendeAdresseBeskyttelse(): PersonInformasjon.Skjerming {
    if (adressebeskyttelse.isEmpty()) return PersonInformasjon.Skjerming.UGRADERT

    val beskyttelse = adressebeskyttelse.firstOrNull { !it.metadata.historisk }

    return when (beskyttelse?.gradering) {
        AdressebeskyttelseGradering.UGRADERT -> PersonInformasjon.Skjerming.UGRADERT
        AdressebeskyttelseGradering.FORTROLIG -> PersonInformasjon.Skjerming.FORTROLIG
        AdressebeskyttelseGradering.STRENGT_FORTROLIG -> PersonInformasjon.Skjerming.STRENGT_FORTROLIG
        AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> PersonInformasjon.Skjerming.STRENGT_FORTROLIG_UTLAND
        AdressebeskyttelseGradering.__UNKNOWN_VALUE, null -> PersonInformasjon.Skjerming.UGRADERT
    }
}
