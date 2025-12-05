package no.nav.persondataapi.service

import no.nav.persondataapi.domene.Grupper
import no.nav.persondataapi.integrasjon.tilgangsmaskin.client.TilgangMaskinResultat
import no.nav.persondataapi.integrasjon.tilgangsmaskin.client.TilgangResultat
import no.nav.persondataapi.rest.domene.PersonIdent
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Component

@Component
class TilgangService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val grupper: Grupper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun harUtvidetTilgang(groups: List<String>): Boolean {
        val utvidetTilgangId = grupper.finnRolleId(AdGrupper.UTVIDET_TILGANG.azureGruoup)
        logger.info("Saksbehandler er medlem av $groups")
        return utvidetTilgangId != null && groups.contains(utvidetTilgangId)
    }

    fun sjekkTilgang(brukerIdent: PersonIdent, saksbehandlerToken: String): Int {
        val resultat = hentTilgangsresultat(brukerIdent, saksbehandlerToken)
        val data = resultat.data
        logger.info("Sjekker tilgang til $brukerIdent, svar er : ${data?.status} - ${data?.title}")
        return beregnStatus(resultat)
    }

    fun hentTilgangsresultat(brukerIdent: PersonIdent, saksbehandlerToken: String): TilgangResultat {
        return tilgangsmaskinClient.sjekkTilgang(brukerIdent, saksbehandlerToken)
    }

    internal fun beregnStatus(resultat: TilgangResultat): Int {
        val data = resultat.data
        return when {
            data?.status == 204 -> 200
            data?.title == "AVVIST_GEOGRAFISK" -> 200
            data?.harTilgangMedBasicAdgang() == true -> {
                logger.info("Overstyrer skjerming for ${resultat.data.brukerIdent} - ${data.title}")
                return 200
            }
            else -> data?.status ?: resultat.statusCode!!
        }
    }

}

interface TilgangsmaskinClient {
    fun sjekkTilgang(personIdent: PersonIdent, saksbehandlerToken: String): TilgangResultat
}

/*
* dette er i praksis en funksjon som overstyrer at du
* har lov til å se aktuell person ut fra kode fra tilgangmaskin
* https://confluence.adeo.no/spaces/TM/pages/628888614/Intro+til+Tilgangsmaskinen
*
* Basic bruker skal ikke ha adgang til AVVIST_STRENGT_FORTROLIG_ADRESSE
* Basic bruker skal ikke ha adgang til AVVIST_STRENGT_FORTROLIG_UTLAND
* Basic bruker skal ikke ha adgang til AVVIST_FORTROLIG_UTLAND
* Basic bruker har tilgang til :
*   - AVVIST_GEOGRAFISK
*   - AVVIST_AVDOED
*   - AVVIST_HABILITET
*   - AVVIST_SKJERMING
*   - AVVIST_VERGE
*   - AVVIST_MANGLENDE_DATA
*   - AVVIST_PERSON_UTLAND
*   - AVVIST_UKJENT_BOSTED
*
* */
fun TilgangMaskinResultat.harTilgangMedBasicAdgang(): Boolean {
    when (this.title) {
        "AVVIST_STRENGT_FORTROLIG_ADRESSE" -> return false  // Saksbehandler har ikke tilgang til brukere med strengt fortrolig adresse.
        "AVVIST_STRENGT_FORTROLIG_UTLAND" -> return false   // Saksbehandler har ikke tilgang til brukere med strengt fortrolig adresse i utlandet.
        "AVVIST_FORTROLIG_ADRESSE" -> return false          // Saksbehandler har ikke tilgang til brukere med fortrolig adresse.
        "AVVIST_GEOGRAFISK" -> return true                  // Saksbehandler har  tilgang til brukerens geografiske område eller enhet.
        "AVVIST_AVDOED" -> return true                      // Saksbehandler har  tilgang til brukere som har vært død i mer enn X måneder.
        "AVVIST_AVDØD" -> return true                       // Saksbehandler har  tilgang til brukere som har vært død i mer enn X måneder.
        "AVVIST_SKJERMING" -> return false                  // Saksbehandler har ikke tilgang til Nav-ansatte og andre skjermede brukere.
        "AVVIST_HABILITET" -> return true                   // Saksbehandler har tilgang til data om seg selv eller sine nærstående.
        "AVVIST_VERGE" -> return true                       // Saksbehandler har tilgang om man er registrert som brukerens verge.
        "AVVIST_MANGLENDE_DATA" -> return true              // Om baksystemer kræsjer, anta at man har tilgang
        "AVVIST_PERSON_UTLAND" -> return true               // Saksbehandler har tilgang om man er registrert i utlandet
        "AVVIST_UKJENT_BOSTED" -> return true               // Saksbehandler har tilgang om man ikke vet bosted
    }
    return false
}

enum class AdGrupper(val azureGruoup: String) {
    UTVIDET_TILGANG("0000-GA-kontroll-Oppslag-Bruker-Utvidet"),
    BASIC_TILGANG("0000-GA-kontroll-Oppslag-Bruker-Basic")
}
