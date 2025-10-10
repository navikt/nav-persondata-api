package no.nav.persondataapi.service

import no.nav.persondataapi.domain.Grupper
import no.nav.persondataapi.integration.tilgangsmaskin.client.TilgangMaskinResultat
import no.nav.persondataapi.integration.tilgangsmaskin.client.TilgangResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TilgangMaskinServiceTest {
    val strengtFortrolig  = "AVVIST_STRENGT_FORTROLIG_ADRESSE"
    val strengtFortroligUtland =  "AVVIST_STRENGT_FORTROLIG_UTLAND"
    val fortroligAdresse =  "AVVIST_FORTROLIG_ADRESSE"
    val egenAnsatt =  "AVVIST_SKJERMING"
    val egneData ="AVVIST_HABILITET"
    val verge = "AVVIST_VERGE"
    val manglendeData = "AVVIST_MANGLENDE_DATA"

    @Test
    fun `Basic adgang skal ikke  ha tilgang til strengt fortrolig`() {
         val client = TilgangsMaskinMockClient()
        client.title=strengtFortrolig
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(403,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal ha tilgang til  fortrolig`() {
        val client = TilgangsMaskinMockClient()
        client.title=fortroligAdresse
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(200,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal ha tilgang til  egen_ansatt`() {
        val client = TilgangsMaskinMockClient()
        client.title=egenAnsatt
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(200,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal ikke ha tilgang til  egne_data`() {
        val client = TilgangsMaskinMockClient()
        client.title=egneData
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(200,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal  ha tilgang til  verge`() {
        val client = TilgangsMaskinMockClient()
        client.title=verge
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(200,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal ikke ha tilgang til  strengt fortrolig_utland`() {
        val client = TilgangsMaskinMockClient()
        client.title=strengtFortroligUtland
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(403,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal ha tilgang til  manglende data`() {
        val client = TilgangsMaskinMockClient()
        client.title=manglendeData
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(200,tms.sjekkTilgang("12345","2222"))
    }
}


class TilgangsMaskinMockClient: TilgangsmaskinClient
{
    var title = ""
    override fun sjekkTilgang(
        fnr: String,
        userToken: String
    ): TilgangResultat {
        return TilgangResultat(
            TilgangMaskinResultat(
                type = null,
                title = title,
                status = 403,
                instance = null,
                brukerIdent = null,
                navIdent = null,
                traceId = null,
                begrunnelse = null,
                kanOverstyres = false
            ),
            200,
        )
    }
}
