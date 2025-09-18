package no.nav.persondataapi.service

import no.nav.persondataapi.kodeverk.client.KodeverkClient
import no.nav.persondataapi.kodeverk.client.Landkode
import org.springframework.stereotype.Component

@Component
class KodeverkService(
    val kodeverkClient: KodeverkClient
) {
    fun mapLandkodeTilLandnavn(landkode: String?): String? =
        kodeverkClient.hentLandkoder().find { landkode == it.landkode }?.land ?: "Ukjent"

}
