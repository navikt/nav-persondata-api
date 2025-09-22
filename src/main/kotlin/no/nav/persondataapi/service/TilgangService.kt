package no.nav.persondataapi.service

import no.nav.persondataapi.domain.Grupper
import no.nav.persondataapi.domain.TilgangMaskinResultat
import no.nav.persondataapi.domain.TilgangResultat

import org.springframework.stereotype.Component

@Component
class TilgangService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val grupper: Grupper)
{
    fun harUtvidetTilgang(groups: List<String>): Boolean {
        val utvidetTilgangId = grupper.finnRolleId(AdGrupper.UTVIDET_TILGANG.azureGruoup)
        return utvidetTilgangId != null && groups.contains(utvidetTilgangId)
    }

    fun sjekkTilgang(fnr: String, userToken: String): Int {
        val resultat = tilgangsmaskinClient.sjekkTilgang(fnr, userToken)
        val data = resultat.data

        return when {
            data?.status == 204 -> 200
            data?.harTilgangMedBasicAdgang() == true -> 200
            else -> data?.status ?: resultat.statusCode!!
        }
    }

}
interface TilgangsmaskinClient {
    fun sjekkTilgang(fnr: String, userToken: String): TilgangResultat
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
fun TilgangMaskinResultat.harTilgangMedBasicAdgang():Boolean{
         when(this.title){
             "AVVIST_STRENGT_FORTROLIG_ADRESSE" -> return false
             "AVVIST_STRENGT_FORTROLIG_UTLAND" -> return false
             "AVVIST_FORTROLIG_ADRESSE" -> return true
             "AVVIST_SKJERMING" -> return true   //Egen Ansatt,
             "AVVIST_HABILITET" -> return true   //Egne data,Egen familie,Verge
             "AVVIST_VERGE" -> return true         //verge
             "AVVIST_MANGLENDE_DATA" -> return true    //Mangler data
         }
    return false
    }

/*
* Funksjon som vurderer hvorvidt geolokasjon skal maskeres før det vises til saksbehandler
* */
fun TilgangMaskinResultat.skalMaskere(harUtvidetTilgang: Boolean):Boolean{
    when(this.title){
        "AVVIST_STRENGT_FORTROLIG_ADRESSE" -> return !harUtvidetTilgang //utvidet adgang skal ikke maskere
        "AVVIST_STRENGT_FORTROLIG_UTLAND" -> return !harUtvidetTilgang  //utvidet adgang skal ikke maskere
        "AVVIST_FORTROLIG_ADRESSE" -> return !harUtvidetTilgang //utvidet adgang skal ikke maskere
        "AVVIST_SKJERMING" -> return false   //Egen Ansatt,
        "AVVIST_HABILITET" -> return false   //Egne data,Egen familie,Verge
        "AVVIST_VERGE" -> return false         //verge
        "AVVIST_MANGLENDE_DATA" -> return false    //Mangler data
    }
    return false
}

enum class AdGrupper(val azureGruoup: String) {
    UTVIDET_TILGANG("0000-GA-kontroll-Oppslag-Bruker-Utvidet"),
    BASIC_TILGANG("0000-GA-kontroll-Oppslag-Bruker-Basic")
}

