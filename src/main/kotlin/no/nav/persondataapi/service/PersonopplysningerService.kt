package no.nav.persondataapi.service

import no.nav.persondataapi.generated.pdl.enums.AdressebeskyttelseGradering
import no.nav.persondataapi.generated.pdl.hentperson.Person
import no.nav.persondataapi.generated.pdl.hentpersonbolk.HentPersonBolkResult
import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.domene.PersonInformasjon
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import no.nav.persondataapi.tracelogging.traceLoggHvisAktivert
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period

@Service
class PersonopplysningerService(
    private val pdlClient: PdlClient,
    private val brukertilgangService: BrukertilgangService,
    private val kodeverkService: KodeverkService,
    private val navTilhørighetService: NavTilhørighetService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun finnesPerson(personIdent: PersonIdent): Boolean {
        val response = pdlClient.hentPerson(personIdent)
        return response.statusCode != 404
    }

    suspend fun hentPersonopplysningerForPerson(
        personIdent: PersonIdent,
        responsLog: Boolean = false,
    ): PersonopplysningerResultat {
        // Hent person fra PDL
        val pdlResponse = pdlClient.hentPerson(personIdent)
        val lokalKontor = navTilhørighetService.finnLokalKontorForPersonIdent(personIdent)
        traceLoggHvisAktivert(
            logger = logger,
            kilde = "PDL hentPerson",
            personIdent = personIdent,
            unit = pdlResponse,
        )
        traceLoggHvisAktivert(
            logger = logger,
            kilde = "PDL lokalKontor",
            personIdent = personIdent,
            unit = lokalKontor,
        )
        logger.info("Hentet personopplysninger for $personIdent, status ${pdlResponse.statusCode}")

        // Håndter feil fra PdlClient
        when (pdlResponse.statusCode) {
            404 -> return PersonopplysningerResultat.PersonIkkeFunnet
            403 -> return PersonopplysningerResultat.IngenTilgang
            500 -> return PersonopplysningerResultat.FeilIBaksystem
            !in 200..299 -> return PersonopplysningerResultat.FeilIBaksystem
        }

        val pdlData = pdlResponse.data ?: return PersonopplysningerResultat.PersonIkkeFunnet

        // Hent navn for familiemedlemmer via ett bolk-kall
        val familiemedlemmerMedRolle: Map<String, String> = byggFamiliemedlemmerMedRolle(pdlData)
        val familiemedlemmer = berikFamiliemedlemmerMedNavn(familiemedlemmerMedRolle)

        // Mappe familie og sivilstand
        val statsborgerskap = pdlData.statsborgerskap.map { it.land }

        // Mappe personopplysninger
        val personopplysninger =
            PersonInformasjon(
                navn =
                    PersonInformasjon.Navn(
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
                alder =
                    pdlData.foedselsdato.firstOrNull()?.foedselsdato?.let {
                        Period.between(LocalDate.parse(it), LocalDate.now()).years
                    } ?: -1,
                fødselsdato = pdlData.foedselsdato.firstOrNull()?.foedselsdato ?: "",
                dødsdato = pdlData.doedsfall.firstOrNull()?.doedsdato,
                navKontor =
                    PersonInformasjon.NavKontor(
                        enhetId = lokalKontor.enhetId,
                        navn = lokalKontor.navn,
                        enhetNr = lokalKontor.enhetNr,
                        type = lokalKontor.type,
                    ),
            )

        // Berik med kodeverkdata
        var beriketPersonopplysninger = berikMedKodeverkData(personopplysninger)

        logger.info("Hentet og mappet personopplysninger for $personIdent")

        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info(
                "Saksbehandler har ikke tilgang til å hente personopplysninger for $personIdent. Maskerer responsen",
            )
            beriketPersonopplysninger = maskerObjekt(beriketPersonopplysninger)
        }

        return PersonopplysningerResultat.Success(beriketPersonopplysninger)
    }

    private fun byggFamiliemedlemmerMedRolle(pdlData: Person): Map<String, String> {
        val foreldreOgBarn =
            pdlData.forelderBarnRelasjon
                .filter { it.relatertPersonsIdent != null }
                .associate { it.relatertPersonsIdent!! to (it.relatertPersonsRolle?.name ?: "Ukjent") }
        val ektefelle =
            pdlData.sivilstand
                .filter { it.relatertVedSivilstand != null }
                .associate { it.relatertVedSivilstand!! to it.type.name }
        return foreldreOgBarn + ektefelle
    }

    private suspend fun berikFamiliemedlemmerMedNavn(
        familiemedlemmerMedRolle: Map<String, String>,
    ): List<PersonInformasjon.Familiemedlem> {
        if (familiemedlemmerMedRolle.isEmpty()) return emptyList()

        val identer = familiemedlemmerMedRolle.keys.map { PersonIdent(it) }
        val bolkResultat = pdlClient.hentPersonBolk(identer)

        val navnPerIdent: Map<String, HentPersonBolkResult> =
            bolkResultat.data
                .filter { it.code == "ok" && it.person != null }
                .associateBy { it.ident }

        return familiemedlemmerMedRolle.map { (ident, rolle) ->
            val bolk = navnPerIdent[ident]
            val navn = bolk?.person?.navn?.firstOrNull()
            val fødselsdato = bolk?.person?.foedselsdato?.firstOrNull()?.foedselsdato
            val gradering =
                bolk?.person?.adressebeskyttelse?.firstOrNull()?.gradering
                    ?: AdressebeskyttelseGradering.UGRADERT
            PersonInformasjon.Familiemedlem(
                ident = ident,
                rolle = rolle,
                fornavn = navn?.fornavn,
                mellomnavn = navn?.mellomnavn,
                etternavn = navn?.etternavn,
                fødselsdato = fødselsdato,
                adresseBeskyttelse = gradering.tilSkjerming(),
            )
        }
    }

    private fun AdressebeskyttelseGradering.tilSkjerming(): PersonInformasjon.Skjerming =
        when (this) {
            AdressebeskyttelseGradering.FORTROLIG -> PersonInformasjon.Skjerming.FORTROLIG
            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> PersonInformasjon.Skjerming.STRENGT_FORTROLIG
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> PersonInformasjon.Skjerming.STRENGT_FORTROLIG_UTLAND
            else -> PersonInformasjon.Skjerming.UGRADERT
        }

    private fun berikMedKodeverkData(input: PersonInformasjon): PersonInformasjon =
        input.copy(
            statsborgerskap = input.statsborgerskap.map { kodeverkService.mapLandkodeTilLandnavn(it) },
            adresse =
                input.adresse?.let { adresse ->
                    adresse.copy(
                        norskAdresse =
                            adresse.norskAdresse?.copy(
                                poststed = kodeverkService.mapPostnummerTilPoststed(adresse.norskAdresse.postnummer),
                            ),
                        utenlandskAdresse =
                            adresse.utenlandskAdresse?.copy(
                                landkode = kodeverkService.mapLandkodeTilLandnavn(adresse.utenlandskAdresse.landkode),
                            ),
                    )
                },
        )
}

sealed class PersonopplysningerResultat {
    data class Success(
        val data: PersonInformasjon,
    ) : PersonopplysningerResultat()

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
