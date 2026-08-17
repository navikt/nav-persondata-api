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
import no.nav.persondataapi.integrasjon.dagpenger.datadeling.DagpengerMeldekortRespons
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.Aktivitet
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.AktivitetType
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.Dag
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.Kilde
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.Meldekort
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.MeldekortStatus
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.MeldekortType
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.domain.AktivitetTypeDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.Periode as DpPeriode

private const val IDENT = "12345678901"

class MeldekortServiceTest {
    // ---------------------------------------------------------------
    // AAP — hentAAPMeldekortForPerson
    // ---------------------------------------------------------------

    @Test
    fun `AAP - mapper reell respons med flere vedtak-elementer som deler samme vedtakId`() {
        val respons = lesAapFixture("testrespons/AAPMaxReponsReellSverdi.json")
        val service = lagService(aapRespons = AapMeldekortRespons(respons.vedtak, 200, null))

        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = true)

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
    fun `AAP - kaster ikke NPE når utbetalingsperiode mangler tilOgMedDato (åpen periode)`() {
        // Regresjonstest for tidligere !!-krasj: domenemodellen Periode har
        // tilOgMedDato som nullable, men den forrige koden brukte !! ved
        // mapping til AapMeldekortPeriode. Kelvin kan i prinsippet returnere
        // en pågående utbetalingsperiode uten kjent sluttdato.
        val vedtak =
            lagVedtak(
                periode = Periode(LocalDate.parse("2026-01-01"), null),
                utbetaling =
                    listOf(
                        lagUtbetaling(
                            periode = Periode(LocalDate.parse("2026-01-01"), null),
                        ),
                    ),
            )
        val service = lagService(aapRespons = AapMeldekortRespons(listOf(vedtak), 200, null))

        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)

        assertTrue(resultat is AAPMeldekortResultat.Success)
        val data = (resultat as AAPMeldekortResultat.Success).data
        assertEquals(1, data.size)
        // Faller tilbake til fraOgMed når tilOgMedDato mangler, i stedet for å krasje.
        assertEquals(LocalDate.parse("2026-01-01"), data[0].perioder[0].tilOgMed)
    }

    @Test
    fun `AAP - håndterer reduksjon lik null uten feil (person har ikke rapportert arbeid)`() {
        val respons = lesAapFixture("testrespons/AAPMaxReponsReellSverdi.json")
        val service = lagService(aapRespons = AapMeldekortRespons(respons.vedtak, 200, null))

        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = true)

        assertTrue(resultat is AAPMeldekortResultat.Success)
        val data = (resultat as AAPMeldekortResultat.Success).data
        assertTrue(data.flatMap { it.perioder }.all { it.arbeidetTimer == null })
    }

    @Test
    fun `AAP - mapper arbeidetTimer og annenReduksjon når reduksjon er satt`() {
        val vedtak =
            lagVedtak(
                utbetaling =
                    listOf(
                        lagUtbetaling(
                            reduksjon = Reduksjon(annenReduksjon = 0.5f, timerArbeidet = 12.5),
                        ),
                    ),
            )
        val service = lagService(aapRespons = AapMeldekortRespons(listOf(vedtak), 200, null))

        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)

        val data = (resultat as AAPMeldekortResultat.Success).data
        assertEquals(12.5, data[0].perioder[0].arbeidetTimer)
        assertEquals(0.5f, data[0].perioder[0].annenReduksjon)
    }

    @Test
    fun `AAP - mapper vedtakPeriode fra vedtakets egen periode, ikke fra utbetalingsperioden`() {
        // vedtakPeriode og utbetalingsperiodene er forskjellige felter i
        // kildedataen (et vedtak kan spenne over lengre tid enn en enkelt
        // utbetaling) — bekreft at de ikke blandes sammen ved mapping.
        val vedtak =
            lagVedtak(
                periode = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-12-31")),
                utbetaling =
                    listOf(
                        lagUtbetaling(
                            periode = Periode(LocalDate.parse("2025-06-01"), LocalDate.parse("2025-06-14")),
                        ),
                    ),
            )
        val service = lagService(aapRespons = AapMeldekortRespons(listOf(vedtak), 200, null))

        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)

        val data = (resultat as AAPMeldekortResultat.Success).data
        assertEquals(LocalDate.parse("2025-01-01"), data[0].vedtakPeriode.fraOgMed)
        assertEquals(LocalDate.parse("2025-12-31"), data[0].vedtakPeriode.tilOgMed)
        assertEquals(LocalDate.parse("2025-06-01"), data[0].perioder[0].fraOgMed)
        assertEquals(LocalDate.parse("2025-06-14"), data[0].perioder[0].tilOgMed)
    }

    @Test
    fun `AAP - mapper rettighetsType, kildesystem, tema og vedtaktypeNavn korrekt`() {
        val vedtak =
            lagVedtak(
                rettighetsType = "SYKEPENGEERSTATNING",
                kildesystem = "KELVIN",
                vedtaksTypeNavn = "Innvilgelse",
            )
        val service = lagService(aapRespons = AapMeldekortRespons(listOf(vedtak), 200, null))

        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)

        val data = (resultat as AAPMeldekortResultat.Success).data
        assertEquals("SYKEPENGEERSTATNING", data[0].rettighetsType)
        assertEquals("KELVIN", data[0].kide)
        assertEquals(Tema.AAP, data[0].tema)
        assertEquals("Innvilgelse", data[0].vedtaktypeNavn)
    }

    @Test
    fun `AAP - flere utbetalingsperioder innenfor ett vedtak mappes som separate perioder`() {
        val vedtak =
            lagVedtak(
                utbetaling =
                    listOf(
                        lagUtbetaling(
                            periode = Periode(LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-31")),
                            utbetalingsgrad = 100,
                        ),
                        lagUtbetaling(
                            periode = Periode(LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-09")),
                            utbetalingsgrad = 0,
                        ),
                    ),
            )
        val service = lagService(aapRespons = AapMeldekortRespons(listOf(vedtak), 200, null))

        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)

        val data = (resultat as AAPMeldekortResultat.Success).data
        assertEquals(2, data[0].perioder.size)
        assertEquals(100, data[0].perioder[0].utbetalingsgrad)
        assertEquals(0, data[0].perioder[1].utbetalingsgrad)
    }

    @Test
    fun `AAP - returnerer PersonIkkeFunnet ved 404`() {
        val service = lagService(aapRespons = AapMeldekortRespons(null, 404, "Ikke funnet"))
        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is AAPMeldekortResultat.PersonIkkeFunnet)
    }

    @Test
    fun `AAP - returnerer IngenTilgang ved 403`() {
        val service = lagService(aapRespons = AapMeldekortRespons(null, 403, "Forbudt"))
        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is AAPMeldekortResultat.IngenTilgang)
    }

    @Test
    fun `AAP - returnerer IngenTilgang ved 401`() {
        val service = lagService(aapRespons = AapMeldekortRespons(null, 401, "Uautorisert"))
        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is AAPMeldekortResultat.IngenTilgang)
    }

    @Test
    fun `AAP - returnerer FeilIBaksystem ved 500`() {
        val service = lagService(aapRespons = AapMeldekortRespons(null, 500, "Feil"))
        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is AAPMeldekortResultat.FeilIBaksystem)
    }

    @Test
    fun `AAP - returnerer FeilIBaksystem ved andre uventede statuskoder (502)`() {
        val service = lagService(aapRespons = AapMeldekortRespons(null, 502, "Gateway"))
        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is AAPMeldekortResultat.FeilIBaksystem)
    }

    @Test
    fun `AAP - returnerer tom liste (ikke feil) når data er null`() {
        val service = lagService(aapRespons = AapMeldekortRespons(null, 200, null))
        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is AAPMeldekortResultat.Success)
        assertTrue((resultat as AAPMeldekortResultat.Success).data.isEmpty())
    }

    @Test
    fun `AAP - returnerer tom liste når data er en tom liste`() {
        val service = lagService(aapRespons = AapMeldekortRespons(emptyList(), 200, null))
        val resultat = service.hentAAPMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is AAPMeldekortResultat.Success)
        assertTrue((resultat as AAPMeldekortResultat.Success).data.isEmpty())
    }

    // ---------------------------------------------------------------
    // Dagpenger — hentDagpengeMeldekortForPerson
    // ---------------------------------------------------------------

    @Test
    fun `Dagpenger - mapper innsendt meldekort korrekt til DTO`() {
        val meldekort =
            lagMeldekort(
                status = MeldekortStatus.Innsendt,
                dager =
                    listOf(
                        Dag(
                            dato = LocalDate.parse("2026-01-01"),
                            dagIndex = 0,
                            aktiviteter =
                                listOf(
                                    Aktivitet(
                                        id = "a1",
                                        type = AktivitetType.Arbeid,
                                        timer = "PT7H30M",
                                        dato = LocalDate.parse("2026-01-01"),
                                    ),
                                ),
                        ),
                    ),
            )
        val service = lagService(dpRespons = DagpengerMeldekortRespons(listOf(meldekort), 200, null))

        val resultat = service.hentDagpengeMeldekortForPerson(PersonIdent(IDENT), utvidet = false)

        assertTrue(resultat is MeldekortResultat.Success)
        val data = (resultat as MeldekortResultat.Success).data
        assertEquals(1, data.size)
        assertEquals(1, data[0].dager.size)
        assertEquals(7.5, data[0].dager[0].aktiviteter[0].timer)
        assertEquals(AktivitetTypeDto.Arbeid, data[0].dager[0].aktiviteter[0].type)
    }

    @Test
    fun `Dagpenger - filtrerer bort meldekort som ikke har status Innsendt`() {
        val innsendt = lagMeldekort(id = "mk-1", status = MeldekortStatus.Innsendt)
        val tilUtfylling = lagMeldekort(id = "mk-2", status = MeldekortStatus.TilUtfylling)
        val service =
            lagService(dpRespons = DagpengerMeldekortRespons(listOf(innsendt, tilUtfylling), 200, null))

        val resultat = service.hentDagpengeMeldekortForPerson(PersonIdent(IDENT), utvidet = false)

        val data = (resultat as MeldekortResultat.Success).data
        assertEquals(1, data.size)
        assertEquals("mk-1", data[0].id)
    }

    @Test
    fun `Dagpenger - returnerer PersonIkkeFunnet ved 404`() {
        val service = lagService(dpRespons = DagpengerMeldekortRespons(null, 404, "Ikke funnet"))
        val resultat = service.hentDagpengeMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is MeldekortResultat.PersonIkkeFunnet)
    }

    @Test
    fun `Dagpenger - returnerer IngenTilgang ved 403`() {
        val service = lagService(dpRespons = DagpengerMeldekortRespons(null, 403, "Forbudt"))
        val resultat = service.hentDagpengeMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is MeldekortResultat.IngenTilgang)
    }

    @Test
    fun `Dagpenger - returnerer FeilIBaksystem ved 500`() {
        val service = lagService(dpRespons = DagpengerMeldekortRespons(null, 500, "Feil"))
        val resultat = service.hentDagpengeMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is MeldekortResultat.FeilIBaksystem)
    }

    @Test
    fun `Dagpenger - returnerer tom liste når data er null`() {
        val service = lagService(dpRespons = DagpengerMeldekortRespons(null, 200, null))
        val resultat = service.hentDagpengeMeldekortForPerson(PersonIdent(IDENT), utvidet = false)
        assertTrue(resultat is MeldekortResultat.Success)
        assertTrue((resultat as MeldekortResultat.Success).data.isEmpty())
    }

    @Test
    fun `Dagpenger - aktivitet uten timer (timer=null) mappes til null, ikke krasj`() {
        val meldekort =
            lagMeldekort(
                dager =
                    listOf(
                        Dag(
                            dato = LocalDate.parse("2026-01-01"),
                            dagIndex = 0,
                            aktiviteter =
                                listOf(
                                    Aktivitet(id = "a1", type = AktivitetType.Fravaer, timer = null, dato = null),
                                ),
                        ),
                    ),
            )
        val service = lagService(dpRespons = DagpengerMeldekortRespons(listOf(meldekort), 200, null))

        val resultat = service.hentDagpengeMeldekortForPerson(PersonIdent(IDENT), utvidet = false)

        val data = (resultat as MeldekortResultat.Success).data
        assertNull(data[0].dager[0].aktiviteter[0].timer)
    }
}

// ---------------------------------------------------------------
// Test-hjelpere
// ---------------------------------------------------------------

private fun lagService(
    aapRespons: AapMeldekortRespons = AapMeldekortRespons(emptyList(), 200, null),
    dpRespons: DagpengerMeldekortRespons = DagpengerMeldekortRespons(emptyList(), 200, null),
    harTilgang: Boolean = true,
): MeldekortService {
    val brukertilgangService = mockk<BrukertilgangService>()
    val aapClient = mockk<AapClient>()
    val dpDatadelingClient = mockk<DagpengerDatadelingClient>()

    every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns harTilgang
    every { aapClient.hentAapMax(any(), any()) } returns aapRespons
    every { dpDatadelingClient.hentDagpengeMeldekort(any(), any()) } returns dpRespons

    return MeldekortService(dpDatadelingClient, aapClient, brukertilgangService)
}

private fun lagVedtak(
    vedtakId: String = "v1",
    status: String = "LØPENDE",
    saksnummer: String = "SAK1",
    periode: Periode = Periode(LocalDate.parse("2025-01-01"), null),
    rettighetsType: String = "BISTANDSBEHOV",
    kildesystem: String = "KELVIN",
    vedtaksTypeNavn: String? = null,
    utbetaling: List<Utbetaling> = listOf(lagUtbetaling()),
): Vedtak =
    Vedtak(
        vedtakId = vedtakId,
        status = status,
        saksnummer = saksnummer,
        vedtaksdato = "2026-01-01",
        periode = periode,
        rettighetsType = rettighetsType,
        dagsats = 1000,
        dagsatsEtterUforeReduksjon = 1000,
        beregningsgrunnlag = 400000,
        barnMedStonad = 0,
        barnetillegg = 0,
        kildesystem = kildesystem,
        samordningsId = null,
        opphorsAarsak = null,
        vedtaksTypeKode = "O",
        vedtaksTypeNavn = vedtaksTypeNavn,
        utbetaling = utbetaling,
    )

private fun lagUtbetaling(
    periode: Periode = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-14")),
    utbetalingsgrad: Int? = 100,
    reduksjon: Reduksjon? = null,
): Utbetaling =
    Utbetaling(
        periode = periode,
        belop = 10000,
        dagsats = 1000,
        utbetalingsgrad = utbetalingsgrad,
        reduksjon = reduksjon,
        barnetilegg = 0,
        barnetillegg = 0,
    )

private fun lagMeldekort(
    id: String = "mk-1",
    status: MeldekortStatus = MeldekortStatus.Innsendt,
    dager: List<Dag> = emptyList(),
): Meldekort =
    Meldekort(
        id = id,
        ident = IDENT,
        status = status,
        type = MeldekortType.Ordinaert,
        periode = DpPeriode(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-14")),
        dager = dager,
        kanSendes = false,
        kanEndres = false,
        kanSendesFra = LocalDate.parse("2026-01-14"),
        sisteFristForTrekk = null,
        opprettetAv = "Dagpenger",
        kilde = Kilde(rolle = "Bruker", ident = IDENT),
        innsendtTidspunkt = LocalDateTime.parse("2026-01-15T09:00:00"),
        registrertArbeidssoker = true,
        meldedato = LocalDate.parse("2026-01-15"),
    )

private fun lesAapFixture(filename: String): AapMaximumRespons {
    val jsonString = lesJsonFraFil(filename)
    return JsonUtils.fromJson(jsonString)
}

private fun lesJsonFraFil(filename: String): String {
    val resource = ClassPathResource(filename)
    return resource.inputStream.use { inputStream ->
        StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
    }
}
