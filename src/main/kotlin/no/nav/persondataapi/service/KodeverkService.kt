package no.nav.persondataapi.service

import no.nav.persondataapi.kodeverk.client.KodeverkClient
import no.nav.persondataapi.kodeverk.client.Landkode
import org.springframework.stereotype.Component

@Component
class KodeverkService(
    private val kodeverkClient: KodeverkClient
) {
    private val landkoder: List<Landkode> = kodeverkClient.hentLandkoder()

    fun mapLandkodeTilLandnavn(landkode: String?): String? {
        return landkode?.let {
            landkoder.find { landkode == it.landkode }?.land ?: "Ukjent"
        }
    }
}
