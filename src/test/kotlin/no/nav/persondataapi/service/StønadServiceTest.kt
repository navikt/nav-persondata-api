package no.nav.persondataapi.service

import io.mockk.every
import io.mockk.mockk
import no.nav.persondataapi.integrasjon.utbetaling.client.UtbetalingClient
import no.nav.persondataapi.integrasjon.utbetaling.client.UtbetalingRespons
import no.nav.persondataapi.integrasjon.utbetaling.client.UtbetalingResultat
import no.nav.persondataapi.integrasjon.utbetaling.dto.Aktoer
import no.nav.persondataapi.integrasjon.utbetaling.dto.Aktoertype
import no.nav.persondataapi.integrasjon.utbetaling.dto.Periode
import no.nav.persondataapi.integrasjon.utbetaling.dto.Utbetaling
import no.nav.persondataapi.integrasjon.utbetaling.dto.Ytelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class StønadServiceTest {

    @Test
    fun `skal returnere IngenTilgang når saksbehandler ikke har tilgang`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns false

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.IngenTilgang)
    }

    @Test
    fun `skal returnere PersonIkkeFunnet når UtbetalingClient returnerer 404`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = null,
            statusCode = 404,
            errorMessage = "Not found"
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.PersonIkkeFunnet)
    }

    @Test
    fun `skal returnere IngenTilgang når UtbetalingClient returnerer 403`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = null,
            statusCode = 403,
            errorMessage = "Forbidden"
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.IngenTilgang)
    }

    @Test
    fun `skal returnere IngenTilgang når UtbetalingClient returnerer 401`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = null,
            statusCode = 401,
            errorMessage = "Unauthorized"
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.IngenTilgang)
    }

    @Test
    fun `skal returnere FeilIBaksystem når UtbetalingClient returnerer 500`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = null,
            statusCode = 500,
            errorMessage = "Internal server error"
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.FeilIBaksystem)
    }

    @Test
    fun `skal returnere FeilIBaksystem når UtbetalingClient returnerer annen feilkode`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = null,
            statusCode = 502,
            errorMessage = "Bad gateway"
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.FeilIBaksystem)
    }

    @Test
    fun `skal returnere tom liste når person ikke har stønader`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = UtbetalingRespons(utbetalinger = emptyList()),
            statusCode = 200,
            errorMessage = null
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.Success)
        val data = (resultat as StønadResultat.Success).data
        assertTrue(data.isEmpty())
    }

    @Test
    fun `skal mappe enkelt ytelse til stønad`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        val ytelse = lagYtelse(
            ytelsestype = "Dagpenger",
            beløp = BigDecimal("10000"),
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
            bilagsnummer = "12345"
        )

        val utbetaling = lagUtbetaling(ytelseListe = listOf(ytelse))

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = UtbetalingRespons(utbetalinger = listOf(utbetaling)),
            statusCode = 200,
            errorMessage = null
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.Success)
        val data = (resultat as StønadResultat.Success).data
        assertEquals(1, data.size)
        assertEquals("Dagpenger", data[0].stonadType)
        assertEquals(1, data[0].perioder.size)
        assertEquals(BigDecimal("10000"), data[0].perioder[0].beløp)
        assertEquals("SOKOS", data[0].perioder[0].kilde)
        assertEquals("12345", data[0].perioder[0].info)
    }

    @Test
    fun `skal gruppere flere perioder av samme ytelsestype`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        val ytelse1 = lagYtelse(
            ytelsestype = "Dagpenger",
            beløp = BigDecimal("10000"),
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31)
        )

        val ytelse2 = lagYtelse(
            ytelsestype = "Dagpenger",
            beløp = BigDecimal("10500"),
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 29)
        )

        val utbetaling = lagUtbetaling(ytelseListe = listOf(ytelse1, ytelse2))

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = UtbetalingRespons(utbetalinger = listOf(utbetaling)),
            statusCode = 200,
            errorMessage = null
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.Success)
        val data = (resultat as StønadResultat.Success).data
        assertEquals(1, data.size)
        assertEquals("Dagpenger", data[0].stonadType)
        assertEquals(2, data[0].perioder.size)
    }

    @Test
    fun `skal håndtere flere forskjellige ytelsestyper`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        val dagpenger = lagYtelse(
            ytelsestype = "Dagpenger",
            beløp = BigDecimal("10000"),
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31)
        )

        val sykepenger = lagYtelse(
            ytelsestype = "Sykepenger",
            beløp = BigDecimal("15000"),
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31)
        )

        val utbetaling = lagUtbetaling(ytelseListe = listOf(dagpenger, sykepenger))

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = UtbetalingRespons(utbetalinger = listOf(utbetaling)),
            statusCode = 200,
            errorMessage = null
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.Success)
        val data = (resultat as StønadResultat.Success).data
        assertEquals(2, data.size)
        assertTrue(data.any { it.stonadType == "Dagpenger" })
        assertTrue(data.any { it.stonadType == "Sykepenger" })
    }

    @Test
    fun `skal filtrere bort ytelser uten ytelsestype`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        val medType = lagYtelse(
            ytelsestype = "Dagpenger",
            beløp = BigDecimal("10000"),
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31)
        )

        val utenType = lagYtelse(
            ytelsestype = null,
            beløp = BigDecimal("5000"),
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31)
        )

        val utbetaling = lagUtbetaling(ytelseListe = listOf(medType, utenType))

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = UtbetalingRespons(utbetalinger = listOf(utbetaling)),
            statusCode = 200,
            errorMessage = null
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.Success)
        val data = (resultat as StønadResultat.Success).data
        assertEquals(1, data.size)
        assertEquals("Dagpenger", data[0].stonadType)
        assertEquals(1, data[0].perioder.size)
    }

    @Test
    fun `skal håndtere flere utbetalinger med ytelser`() {
        val brukertilgangService = mockk<BrukertilgangService>()
        val utbetalingClient = mockk<UtbetalingClient>()

        val ytelse1 = lagYtelse(
            ytelsestype = "Dagpenger",
            beløp = BigDecimal("10000"),
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31)
        )

        val ytelse2 = lagYtelse(
            ytelsestype = "Dagpenger",
            beløp = BigDecimal("10500"),
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 29)
        )

        val utbetaling1 = lagUtbetaling(ytelseListe = listOf(ytelse1))
        val utbetaling2 = lagUtbetaling(ytelseListe = listOf(ytelse2))

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { utbetalingClient.hentUtbetalingerForBruker(any()) } returns UtbetalingResultat(
            data = UtbetalingRespons(utbetalinger = listOf(utbetaling1, utbetaling2)),
            statusCode = 200,
            errorMessage = null
        )

        val service = StønadService(utbetalingClient, brukertilgangService)
        val resultat = service.hentStønaderForPerson("12345678901")

        assertTrue(resultat is StønadResultat.Success)
        val data = (resultat as StønadResultat.Success).data
        assertEquals(1, data.size)
        assertEquals("Dagpenger", data[0].stonadType)
        assertEquals(2, data[0].perioder.size)
    }
}

// Hjelpefunksjoner for å lage testdata
private fun lagYtelse(
    ytelsestype: String?,
    beløp: BigDecimal,
    fom: LocalDate,
    tom: LocalDate,
    bilagsnummer: String? = null
): Ytelse {
    val rettighetshaver = Aktoer(
        aktoertype = Aktoertype.PERSON,
        ident = "12345678901",
        navn = "Test Testesen"
    )

    return Ytelse(
        ytelsestype = ytelsestype,
        ytelsesperiode = Periode(fom = fom, tom = tom),
        ytelseNettobeloep = beløp,
        rettighetshaver = rettighetshaver,
        skattsum = BigDecimal.ZERO,
        trekksum = BigDecimal.ZERO,
        ytelseskomponentersum = beløp,
        skattListe = null,
        trekkListe = null,
        ytelseskomponentListe = null,
        bilagsnummer = bilagsnummer,
        refundertForOrg = null
    )
}

private fun lagUtbetaling(ytelseListe: List<Ytelse>): Utbetaling {
    val utbetaltTil = Aktoer(
        aktoertype = Aktoertype.PERSON,
        ident = "12345678901",
        navn = "Test Testesen"
    )

    return Utbetaling(
        utbetaltTil = utbetaltTil,
        utbetalingsmetode = "Bankoverføring",
        utbetalingsstatus = "Utbetalt",
        posteringsdato = LocalDate.now(),
        forfallsdato = LocalDate.now(),
        utbetalingsdato = LocalDate.now(),
        utbetalingNettobeloep = BigDecimal.ZERO,
        utbetalingsmelding = null,
        utbetaltTilKonto = null,
        ytelseListe = ytelseListe
    )
}
