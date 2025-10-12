package no.nav.persondataapi.service

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.inntekt.generated.model.HistorikkData
import no.nav.inntekt.generated.model.Inntektsinformasjon
import no.nav.inntekt.generated.model.InntektshistorikkApiUt
import no.nav.inntekt.generated.model.Loennsinntekt
import no.nav.persondataapi.integrasjon.ereg.client.EregClient
import no.nav.persondataapi.integrasjon.ereg.client.EregRespons
import no.nav.persondataapi.integrasjon.ereg.client.Navn
import no.nav.persondataapi.integrasjon.ereg.client.PeriodeDato
import no.nav.persondataapi.integrasjon.ereg.client.PeriodeTid
import no.nav.persondataapi.integrasjon.inntekt.client.InntektClient
import no.nav.persondataapi.integrasjon.inntekt.client.InntektDataResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

class InntektServiceTest {

    @Test
    fun `skal returnere IngenTilgang når saksbehandler ikke har tilgang`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val inntektClient = mockk<InntektClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns false

        val service = InntektService(inntektClient, eregClient, brukertilgangService)
        val resultat = service.hentInntekterForPerson("12345678901")

        assertTrue(resultat is InntektResultat.IngenTilgang)
    }

    @Test
    fun `skal returnere PersonIkkeFunnet når InntektClient returnerer 404`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val inntektClient = mockk<InntektClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { inntektClient.hentInntekter(any(), any()) } returns InntektDataResultat(
            data = null,
            statusCode = 404,
            errorMessage = "Not found"
        )

        val service = InntektService(inntektClient, eregClient, brukertilgangService)
        val resultat = service.hentInntekterForPerson("12345678901")

        assertTrue(resultat is InntektResultat.PersonIkkeFunnet)
    }

    @Test
    fun `skal returnere IngenTilgang når InntektClient returnerer 403`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val inntektClient = mockk<InntektClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { inntektClient.hentInntekter(any(), any()) } returns InntektDataResultat(
            data = null,
            statusCode = 403,
            errorMessage = "Forbidden"
        )

        val service = InntektService(inntektClient, eregClient, brukertilgangService)
        val resultat = service.hentInntekterForPerson("12345678901")

        assertTrue(resultat is InntektResultat.IngenTilgang)
    }

    @Test
    fun `skal returnere FeilIBaksystem når InntektClient returnerer 500`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val inntektClient = mockk<InntektClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { inntektClient.hentInntekter(any(), any()) } returns InntektDataResultat(
            data = null,
            statusCode = 500,
            errorMessage = "Internal server error"
        )

        val service = InntektService(inntektClient, eregClient, brukertilgangService)
        val resultat = service.hentInntekterForPerson("12345678901")

        assertTrue(resultat is InntektResultat.FeilIBaksystem)
    }

    @Test
    fun `skal returnere FeilIBaksystem når InntektClient returnerer annen feilkode`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val inntektClient = mockk<InntektClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { inntektClient.hentInntekter(any(), any()) } returns InntektDataResultat(
            data = null,
            statusCode = 502,
            errorMessage = "Bad gateway"
        )

        val service = InntektService(inntektClient, eregClient, brukertilgangService)
        val resultat = service.hentInntekterForPerson("12345678901")

        assertTrue(resultat is InntektResultat.FeilIBaksystem)
    }

    @Test
    fun `skal returnere tom liste når person ikke har inntekter`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val inntektClient = mockk<InntektClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { inntektClient.hentInntekter(any(), any()) } returns InntektDataResultat(
            data = InntektshistorikkApiUt(data = emptyList()),
            statusCode = 200,
            errorMessage = null
        )

        val service = InntektService(inntektClient, eregClient, brukertilgangService)
        val resultat = service.hentInntekterForPerson("12345678901")

        assertTrue(resultat is InntektResultat.Success)
        val data = (resultat as InntektResultat.Success).data
        assertTrue(data.lønnsinntekt.isEmpty())
    }

    @Test
    fun `skal mappe lønnsinntekt med organisasjonsinformasjon fra Ereg`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val inntektClient = mockk<InntektClient>()
        val eregClient = mockk<EregClient>()

        val lønnsinntekt = lagLønnsinntekt(
            beskrivelse = "Fastlønn",
            beloep = BigDecimal("50000"),
            antall = BigDecimal("1")
        )

        val historikk = lagHistorikkData(
            opplysningspliktig = "999888777",
            maaned = "2024-01",
            inntektListe = listOf(lønnsinntekt)
        )

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { inntektClient.hentInntekter(any(), any()) } returns InntektDataResultat(
            data = InntektshistorikkApiUt(data = listOf(historikk)),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon("999888777") } returns lagEregRespons("999888777", "Test Bedrift AS")

        val service = InntektService(inntektClient, eregClient, brukertilgangService)
        val resultat = service.hentInntekterForPerson("12345678901")

        assertTrue(resultat is InntektResultat.Success)
        val data = (resultat as InntektResultat.Success).data
        assertEquals(1, data.lønnsinntekt.size)
        assertEquals("Test Bedrift AS", data.lønnsinntekt[0].arbeidsgiver)
        assertEquals("2024-01", data.lønnsinntekt[0].periode)
        assertEquals("Fastlønn", data.lønnsinntekt[0].lønnstype)
        assertEquals(BigDecimal("50000"), data.lønnsinntekt[0].beløp)
        assertEquals(BigDecimal("1"), data.lønnsinntekt[0].antall)
    }

    @Test
    fun `skal håndtere ukjent organisasjon når Ereg ikke har data`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val inntektClient = mockk<InntektClient>()
        val eregClient = mockk<EregClient>()

        val lønnsinntekt = lagLønnsinntekt(
            beskrivelse = "Fastlønn",
            beloep = BigDecimal("50000"),
            antall = BigDecimal("1")
        )

        val historikk = lagHistorikkData(
            opplysningspliktig = "999888777",
            maaned = "2024-01",
            inntektListe = listOf(lønnsinntekt)
        )

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { inntektClient.hentInntekter(any(), any()) } returns InntektDataResultat(
            data = InntektshistorikkApiUt(data = listOf(historikk)),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", null)

        val service = InntektService(inntektClient, eregClient, brukertilgangService)
        val resultat = service.hentInntekterForPerson("12345678901")

        assertTrue(resultat is InntektResultat.Success)
        val data = (resultat as InntektResultat.Success).data
        assertEquals(1, data.lønnsinntekt.size)
        assertEquals(null, data.lønnsinntekt[0].arbeidsgiver)
    }

    @Test
    fun `skal velge nyeste versjon av inntektsinformasjon`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val inntektClient = mockk<InntektClient>()
        val eregClient = mockk<EregClient>()

        val eldre = lagLønnsinntekt(
            beskrivelse = "Gammel lønn",
            beloep = BigDecimal("40000")
        )

        val nyere = lagLønnsinntekt(
            beskrivelse = "Ny lønn",
            beloep = BigDecimal("50000")
        )

        val historikk = lagHistorikkData(
            opplysningspliktig = "999888777",
            maaned = "2024-01",
            versjoner = listOf(
                lagInntektsinformasjon(
                    oppsummeringstidspunkt = OffsetDateTime.parse("2024-02-01T12:00:00+01:00"),
                    inntektListe = listOf(eldre)
                ),
                lagInntektsinformasjon(
                    oppsummeringstidspunkt = OffsetDateTime.parse("2024-01-01T12:00:00+01:00"),
                    inntektListe = listOf(nyere)
                )
            )
        )

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { inntektClient.hentInntekter(any(), any()) } returns InntektDataResultat(
            data = InntektshistorikkApiUt(data = listOf(historikk)),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", null)

        val service = InntektService(inntektClient, eregClient, brukertilgangService)
        val resultat = service.hentInntekterForPerson("12345678901")

        assertTrue(resultat is InntektResultat.Success)
        val data = (resultat as InntektResultat.Success).data
        assertEquals(1, data.lønnsinntekt.size)
        // nyeste() velger den med minst oppsummeringstidspunkt
        assertEquals("Ny lønn", data.lønnsinntekt[0].lønnstype)
        assertEquals(BigDecimal("50000"), data.lønnsinntekt[0].beløp)
    }

    @Test
    fun `skal håndtere flere historikkperioder for samme person`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val inntektClient = mockk<InntektClient>()
        val eregClient = mockk<EregClient>()

        val lønn1 = lagLønnsinntekt(
            beskrivelse = "Fastlønn januar",
            beloep = BigDecimal("50000")
        )

        val lønn2 = lagLønnsinntekt(
            beskrivelse = "Fastlønn februar",
            beloep = BigDecimal("51000")
        )

        val historikk1 = lagHistorikkData(
            opplysningspliktig = "999888777",
            maaned = "2024-01",
            inntektListe = listOf(lønn1)
        )

        val historikk2 = lagHistorikkData(
            opplysningspliktig = "999888777",
            maaned = "2024-02",
            inntektListe = listOf(lønn2)
        )

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { inntektClient.hentInntekter(any(), any()) } returns InntektDataResultat(
            data = InntektshistorikkApiUt(data = listOf(historikk1, historikk2)),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", "Test Bedrift AS")

        val service = InntektService(inntektClient, eregClient, brukertilgangService)
        val resultat = service.hentInntekterForPerson("12345678901")

        assertTrue(resultat is InntektResultat.Success)
        val data = (resultat as InntektResultat.Success).data
        assertEquals(2, data.lønnsinntekt.size)
        assertEquals("2024-01", data.lønnsinntekt[0].periode)
        assertEquals("2024-02", data.lønnsinntekt[1].periode)
    }
}

// Hjelpefunksjoner for å lage testdata
private fun lagLønnsinntekt(
    beskrivelse: String,
    beloep: BigDecimal,
    antall: BigDecimal? = null
): Loennsinntekt {
    return Loennsinntekt(
        beloep = beloep,
        fordel = "kontantytelse",
        beskrivelse = beskrivelse,
        inngaarIGrunnlagForTrekk = true,
        utloeserArbeidsgiveravgift = true,
        antall = antall
    )
}

private fun lagInntektsinformasjon(
    oppsummeringstidspunkt: OffsetDateTime = OffsetDateTime.now(),
    inntektListe: List<no.nav.inntekt.generated.model.Inntekt> = emptyList()
): Inntektsinformasjon {
    return Inntektsinformasjon(
        maaned = "2024-01",
        opplysningspliktig = "999888777",
        underenhet = "999888777",
        norskident = "12345678901",
        oppsummeringstidspunkt = oppsummeringstidspunkt,
        inntektListe = inntektListe
    )
}

private fun lagHistorikkData(
    opplysningspliktig: String,
    maaned: String,
    inntektListe: List<no.nav.inntekt.generated.model.Inntekt> = emptyList(),
    versjoner: List<Inntektsinformasjon>? = null
): HistorikkData {
    val actualVersjoner = versjoner ?: listOf(
        lagInntektsinformasjon(
            inntektListe = inntektListe
        )
    )

    return HistorikkData(
        maaned = maaned,
        opplysningspliktig = opplysningspliktig,
        underenhet = opplysningspliktig,
        norskident = "12345678901",
        versjoner = actualVersjoner
    )
}

private fun lagEregRespons(orgnummer: String, navn: String?): EregRespons {
    return EregRespons(
        organisasjonsnummer = orgnummer,
        type = "AS",
        navn = if (navn != null) {
            Navn(
                sammensattnavn = navn,
                navnelinje1 = navn,
                bruksperiode = PeriodeTid(fom = LocalDateTime.now()),
                gyldighetsperiode = PeriodeDato(fom = LocalDate.now())
            )
        } else null,
        organisasjonDetaljer = null,
        virksomhetDetaljer = null
    )
}
