package no.nav.persondataapi.service

import no.nav.persondataapi.domain.Grupper
import no.nav.persondataapi.domain.TilgangMaskinResultat
import no.nav.persondataapi.domain.TilgangResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TilgangMaskinServiceTest {
    val strengt_fortrolig  = "AVVIST_STRENGT_FORTROLIG_ADRESSE"
    val strengt_fortrolig_utland =  "AVVIST_STRENGT_FORTROLIG_UTLAND"
    val fortrolig_adresse =  "AVVIST_FORTROLIG_ADRESSE"
    val egen_ansatt =  "AVVIST_SKJERMING"
    val egne_data ="AVVIST_HABILITET"
    val verge = "AVVIST_VERGE"
    val manglende_data = "AVVIST_MANGLENDE_DATA"

    @Test
    fun `Basic adgang skal ikke  ha tilgang til strengt fortrolig`() {
         val client = TilgangsMaskinMockClient()
        client.title=strengt_fortrolig
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(403,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal ha tilgang til  fortrolig`() {
        val client = TilgangsMaskinMockClient()
        client.title=fortrolig_adresse
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(200,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal ha tilgang til  egen_ansatt`() {
        val client = TilgangsMaskinMockClient()
        client.title=egen_ansatt
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(200,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal ikke ha tilgang til  egne_data`() {
        val client = TilgangsMaskinMockClient()
        client.title=egne_data
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(403,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal  ha tilgang til  verge`() {
        val client = TilgangsMaskinMockClient()
        client.title=verge
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(403,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal ikke ha tilgang til  strengt fortrolig_utland`() {
        val client = TilgangsMaskinMockClient()
        client.title=strengt_fortrolig_utland
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(403,tms.sjekkTilgang("12345","2222"))
    }
    @Test
    fun `Basic adgang skal ha tilgang til  manglende data`() {
        val client = TilgangsMaskinMockClient()
        client.title=manglende_data
        val tms = TilgangService(client, grupper = Grupper("[]"))

        Assertions.assertEquals(403,tms.sjekkTilgang("12345","2222"))
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