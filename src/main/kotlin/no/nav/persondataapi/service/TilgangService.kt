package no.nav.persondataapi.service

import no.nav.persondataapi.domain.Grupper
import no.nav.persondataapi.tilgangsmaskin.client.TilgangsmaskinClient
import org.springframework.stereotype.Component

@Component
class TilgangService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val grupper: Grupper
) {
    fun harUtvidetTilgang(groups: List<String>): Boolean {
        val utvidetTilgangId = grupper.finnRolleId("0000-GA-kontroll-Oppslag-Bruker-Utvidet")
        return utvidetTilgangId != null && groups.contains(utvidetTilgangId)
    }

    fun sjekkTilgang(fnr: String, userToken: String): Int {
        val resultat = tilgangsmaskinClient.sjekkTilgang(fnr, userToken)
        if (resultat.data!= null && resultat.data.status == 204){
            return 200
        }
        else{
            return resultat.data?.status ?: resultat.statusCode!!      
        }

    }
}
