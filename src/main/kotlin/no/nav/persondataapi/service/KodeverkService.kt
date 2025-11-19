package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.kodeverk.client.KodeverkClient
import org.springframework.stereotype.Component

@Component
class KodeverkService(
    val kodeverkClient: KodeverkClient
) {
    fun mapLandkodeTilLandnavn(landkode: String?): String =
        kodeverkClient.hentLandkoder().find { landkode == it.landkode }?.land ?: "Ukjent"

    fun mapPostnummerTilPoststed(postnummer: String?): String =
        ""

}
