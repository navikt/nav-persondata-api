package no.nav.persondataapi.service

import io.mockk.every
import io.mockk.mockk
import no.nav.persondataapi.integrasjon.aap.meldekort.client.AapClient
import no.nav.persondataapi.integrasjon.aap.meldekort.client.AapMeldekortRespons
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.AapMaximumRespons
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.Periode
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.Reduksjon
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.Utbetaling
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.Vedtak
import no.nav.persondataapi.integrasjon.dagpenger.datadeling.DagpengerDatadelingClient
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.rest.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets
import java.time.LocalDate

/**
 * Tester basert på ekte respons fra AAP-API-intern sitt /maksimum-endepunkt
 * (dev-gcp), hentet manuelt via azure-token-generator + Swagger UI for en
 * reell Kelvin-testperson. Se testrespons/AAPMaxReponsReellSverdi.json.
 */
class MeldekortServiceTest {
    @Test
    fun `mapper reell AAP-respons med flere vedtak-elementer som deler samme vedtakId`() {
        val jsonString = lesJsonFraFil("testrespons/AAPMaxReponsReellSverdi.json")
        val respons: AapMaximumRespons = JsonUtils.fromJson(jsonString)

        val brukertilgangService = mockk<BrukertilgangService>()
        val aapClient = mockk<AapClient>()
        val dpDatadelingClient = mockk<DagpengerDatadelingClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aapClient.hentAapMax(any(), any()) } returns
            AapMeldekortRespons(data = respons.vedtak, statusCode = 200, message = null)

        val service = MeldekortService(dpDatadelingClient, aapClient, brukertilgangService)
        val resultat = service.hentAAPMeldekortForPerson(PersonIdent("12345678901"), utvidet = true)

        assertTrue(resultat is AAPMeldekortResultat.Success)
        val data = (resultat as AAPMeldekortResultat.Success).data

        // Kelvin splitter responsen i flere elementer med SAMME vedtakId når
        // f.eks. dagsatsen endres (G-regulering) eller rettighetsType endres
        // midt i vedtaksperioden. Alle 5 elementene skal mappes uavkortet —
        // ingen deduplisering på vedtakId.
        assertEquals(5, data.size)
        assertTrue(data.all { it.vedtakId == "111603" })
        assertEquals(1, data.count { it.perioder.size == 2 }) // siste vedtak har 2 utbetalingsperioder
    }

    @Test
    fun `kaster ikke NPE når utbetalingsperiode mangler tilOgMedDato (åpen periode)`() {
        // Regresjonstest for tidligere !!-krasj: domenemodellen Periode har
        // tilOgMedDato som nullable, men den forrige koden brukte !! ved
        // mapping til AapMeldekortPeriode. Kelvin kan i prinsippet returnere
        // en pågående utbetalingsperiode uten kjent sluttdato.
        val vedtakUtenSluttdato =
            Vedtak(
                vedtakId = "v-apen",
                status = "LØPENDE",
                saksnummer = "SAK-APEN",
                vedtaksdato = "2026-01-01",
                periode = Periode(LocalDate.parse("2026-01-01"), null),
                rettighetsType = "BISTANDSBEHOV",
                dagsats = 1000,
                dagsatsEtterUforeReduksjon = 1000,
                beregningsgrunnlag = 400000,
                barnMedStonad = 0,
                barnetillegg = 0,
                kildesystem = "KELVIN",
                samordningsId = null,
                opphorsAarsak = null,
                vedtaksTypeKode = "O",
                vedtaksTypeNavn = null,
                utbetaling =
                    listOf(
                        Utbetaling(
                            periode = Periode(LocalDate.parse("2026-01-01"), null),
                            belop = 20000,
                            dagsats = 1000,
                            utbetalingsgrad = 100,
                            reduksjon = Reduksjon(annenReduksjon = null, timerArbeidet = null),
                            barnetilegg = 0,
                            barnetillegg = 0,
                        ),
                    ),
            )

        val brukertilgangService = mockk<BrukertilgangService>()
        val aapClient = mockk<AapClient>()
        val dpDatadelingClient = mockk<DagpengerDatadelingClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aapClient.hentAapMax(any(), any()) } returns
            AapMeldekortRespons(data = listOf(vedtakUtenSluttdato), statusCode = 200, message = null)

        val service = MeldekortService(dpDatadelingClient, aapClient, brukertilgangService)
        val resultat = service.hentAAPMeldekortForPerson(PersonIdent("12345678901"), utvidet = false)

        assertTrue(resultat is AAPMeldekortResultat.Success)
        val data = (resultat as AAPMeldekortResultat.Success).data
        assertEquals(1, data.size)
        // Faller tilbake til fraOgMed når tilOgMedDato mangler, i stedet for å krasje.
        assertEquals(LocalDate.parse("2026-01-01"), data[0].perioder[0].tilOgMed)
    }

    @Test
    fun `håndterer reduksjon lik null uten feil (person har ikke rapportert arbeid)`() {
        // I den reelle responsen er reduksjon alltid null når personen ikke
        // har rapportert arbeid i perioden — skal IKKE krasje, og
        // arbeidetTimer/annenReduksjon skal bli null (ikke 0).
        val jsonString = lesJsonFraFil("testrespons/AAPMaxReponsReellSverdi.json")
        val respons: AapMaximumRespons = JsonUtils.fromJson(jsonString)

        val brukertilgangService = mockk<BrukertilgangService>()
        val aapClient = mockk<AapClient>()
        val dpDatadelingClient = mockk<DagpengerDatadelingClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aapClient.hentAapMax(any(), any()) } returns
            AapMeldekortRespons(data = respons.vedtak, statusCode = 200, message = null)

        val service = MeldekortService(dpDatadelingClient, aapClient, brukertilgangService)
        val resultat = service.hentAAPMeldekortForPerson(PersonIdent("12345678901"), utvidet = true)

        val data = (resultat as AAPMeldekortResultat.Success).data
        assertTrue(data.flatMap { it.perioder }.all { it.arbeidetTimer == null })
    }
}

private fun lesJsonFraFil(filename: String): String {
    val resource = ClassPathResource(filename)
    val inputStream = resource.inputStream
    return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
}
