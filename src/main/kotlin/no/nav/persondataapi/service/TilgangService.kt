package no.nav.persondataapi.service

import no.nav.persondataapi.domene.Grupper
import no.nav.persondataapi.integrasjon.tilgangsmaskin.client.TilgangMaskinResultat
import no.nav.persondataapi.integrasjon.tilgangsmaskin.client.TilgangResultat
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Component

@Component
class TilgangService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val grupper: Grupper
) {
    fun harUtvidetTilgang(groups: List<String>): Boolean {
        val utvidetTilgangId = grupper.finnRolleId(AdGrupper.UTVIDET_TILGANG.azureGruoup)
        return utvidetTilgangId != null && groups.contains(utvidetTilgangId)
    }

    fun sjekkTilgang(brukerIdent: String, saksbehandlerToken: String): Int {
        val resultat = tilgangsmaskinClient.sjekkTilgang(brukerIdent, saksbehandlerToken)
        val data = resultat.data

        return when {
            data?.status == 204 -> 200
            data?.harTilgangMedBasicAdgang() == true -> 200
            else -> data?.status ?: resultat.statusCode!!
        }
    }

}

interface TilgangsmaskinClient {
    fun sjekkTilgang(personIdent: String, saksbehandlerToken: String): TilgangResultat
}

/*
* dette er i praksis en funksjon som overstyrer at du
* har lov til å se aktuell person ut fra kode fra tilgangmaskin
* https://confluence.adeo.no/spaces/TM/pages/628888614/Intro+til+Tilgangsmaskinen
*
* Basic bruker skal ikke ha adgang til AVVIST_STRENGT_FORTROLIG_ADRESSE
* Basic bruker skal ikke ha adgang til AVVIST_STRENGT_FORTROLIG_UTLAND
* Basic bruker har tilgang til :
*   - AVVIST_FORTROLIG_ADRESSE
*   - AVVIST_SKJERMING
*   - AVVIST_HABILITET
*   - AVVIST_VERGE
*   - AVVIST_MANGLENDE_DATA
*
* */
fun TilgangMaskinResultat.harTilgangMedBasicAdgang(): Boolean {
    when (this.title) {
        "AVVIST_STRENGT_FORTROLIG_ADRESSE" -> return false  // Saksbehandler har ikke tilgang til brukere med strengt fortrolig adresse.
        "AVVIST_STRENGT_FORTROLIG_UTLAND" -> return false   // Saksbehandler har ikke tilgang til brukere med strengt fortrolig adresse i utlandet.
        "AVVIST_FORTROLIG_ADRESSE" -> return true           // Saksbehandler har tilgang til brukere med fortrolig adresse.
        "AVVIST_GEOGRAFISK" -> return false                 // Saksbehandler har ikke tilgang til brukerens geografiske område eller enhet.
        "AVVIST_AVDOED" -> return false                     // Saksbehandler har ikke tilgang til brukere som har vært død i mer enn X måneder.
        "AVVIST_SKJERMING" -> return true                   // Saksbehandler har tilgang til Nav-ansatte og andre skjermede brukere.
        "AVVIST_HABILITET" -> return true                   // Saksbehandler har tilgang til data om seg selv eller sine nærstående.
        "AVVIST_VERGE" -> return true                       // Saksbehandler har tilgang om man er registrert som brukerens verge.
        "AVVIST_MANGLENDE_DATA" -> return true              // Om baksystemer kræsjer, anta at man har tilgang
    }
    return false
}

enum class AdGrupper(val azureGruoup: String) {
    UTVIDET_TILGANG("0000-GA-kontroll-Oppslag-Bruker-Utvidet"),
    BASIC_TILGANG("0000-GA-kontroll-Oppslag-Bruker-Basic")
}
