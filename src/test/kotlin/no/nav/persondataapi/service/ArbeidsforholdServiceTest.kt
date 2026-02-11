package no.nav.persondataapi.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.integrasjon.aareg.client.AaregClient
import no.nav.persondataapi.integrasjon.aareg.client.AaregDataResultat
import no.nav.persondataapi.integrasjon.aareg.client.Arbeidsforhold
import no.nav.persondataapi.integrasjon.aareg.client.Arbeidssted
import no.nav.persondataapi.integrasjon.aareg.client.Ansettelsesdetaljer
import no.nav.persondataapi.integrasjon.aareg.client.Ansettelsesperiode
import no.nav.persondataapi.integrasjon.aareg.client.Bruksperiode
import no.nav.persondataapi.integrasjon.aareg.client.Ident
import no.nav.persondataapi.integrasjon.aareg.client.Identer
import no.nav.persondataapi.integrasjon.aareg.client.Identtype
import no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet
import no.nav.persondataapi.integrasjon.aareg.client.Opplysningspliktig
import no.nav.persondataapi.integrasjon.aareg.client.Rapporteringsmaaneder
import no.nav.persondataapi.integrasjon.ereg.client.EregClient
import no.nav.persondataapi.integrasjon.ereg.client.EregRespons
import no.nav.persondataapi.rest.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.test.assertNotEquals

class ArbeidsforholdServiceTest {

    @Test
    fun `skal maskere data når saksbehandler ikke har tilgang`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        val arbeidsforhold = lagArbeidsforhold("999888777", sluttdato = null)

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns false
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = listOf(arbeidsforhold),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", "Test Bedrift AS")

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        // Data skal være maskert - listene finnes men @Maskert-felter er erstattet med *******
        assertEquals(1, data.løpendeArbeidsforhold.size)
        assertEquals("*******", data.løpendeArbeidsforhold[0].arbeidsgiver)
        assertEquals("*******", data.løpendeArbeidsforhold[0].organisasjonsnummer)
    }

    @Test
    fun `skal ha unike id på ulike arbeidsforhold også når saksbehandler ikke har tilgang`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        val arbeidsforhold = lagArbeidsforhold("999888777", sluttdato = null)

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns false
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = listOf(arbeidsforhold),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", "Test Bedrift AS")

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        // Data skal være maskert - listene finnes men @Maskert-felter er erstattet med *******
        assertEquals(1, data.løpendeArbeidsforhold.size)
        assertEquals("*******", data.løpendeArbeidsforhold[0].arbeidsgiver)
        assertEquals("*******", data.løpendeArbeidsforhold[0].organisasjonsnummer)
        assertNotEquals("*******",data.løpendeArbeidsforhold[0].id)
    }
    @Test
    fun `skal ha lik id på like arbeidsforhold også  når saksbehandler ikke har tilgang`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        val arbeidsforhold = lagArbeidsforhold("999888777", sluttdato = null)
        val arbeidsforhold2 = lagArbeidsforhold("999888777", sluttdato = null)

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns false
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = listOf(arbeidsforhold,arbeidsforhold2),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", "Test Bedrift AS")

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        // Data skal være maskert - listene finnes men @Maskert-felter er erstattet med *******
        assertEquals(2, data.løpendeArbeidsforhold.size)
        assertEquals("*******", data.løpendeArbeidsforhold[0].arbeidsgiver)
        assertEquals("*******", data.løpendeArbeidsforhold[0].organisasjonsnummer)
        assertNotEquals("*******",data.løpendeArbeidsforhold[0].id)
        assertEquals(data.løpendeArbeidsforhold[0].id,data.løpendeArbeidsforhold[1].id)
    }
    @Test
    fun `skal ha ullik id på uklike arbeidsforhold også når saksbehandler ikke har tilgang`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        val arbeidsforhold = lagArbeidsforhold("999888777", sluttdato = null)
        val arbeidsforhold2 = lagArbeidsforhold("999888778", sluttdato = null)

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns false
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = listOf(arbeidsforhold,arbeidsforhold2),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon("999888777") } returns lagEregRespons("999888777", "Test Bedrift AS")
        every { eregClient.hentOrganisasjon("999888778") } returns lagEregRespons("999888778", "Test Bedrift AS2")

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        // Data skal være maskert - listene finnes men @Maskert-felter er erstattet med *******
        assertEquals(2, data.løpendeArbeidsforhold.size)
        assertEquals("*******", data.løpendeArbeidsforhold[0].arbeidsgiver)
        assertEquals("*******", data.løpendeArbeidsforhold[0].organisasjonsnummer)
        assertNotEquals("*******",data.løpendeArbeidsforhold[0].id)
        assertNotEquals(data.løpendeArbeidsforhold[0].id,data.løpendeArbeidsforhold[1].id)
    }

    @Test
    fun `skal returnere PersonIkkeFunnet når Aareg returnerer 404`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = emptyList(),
            statusCode = 404,
            errorMessage = "Not found"
        )

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.PersonIkkeFunnet)
    }

    @Test
    fun `skal returnere IngenTilgang når Aareg returnerer 403`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = emptyList(),
            statusCode = 403,
            errorMessage = "Forbidden"
        )

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.IngenTilgang)
    }

    @Test
    fun `skal returnere FeilIBaksystem når Aareg returnerer 500`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = emptyList(),
            statusCode = 500,
            errorMessage = "Internal server error"
        )

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.FeilIBaksystem)
    }

    @Test
    fun `skal returnere FeilIBaksystem når Aareg returnerer annen feilkode`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = emptyList(),
            statusCode = 502,
            errorMessage = "Bad gateway"
        )

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.FeilIBaksystem)
    }

    @Test
    fun `skal returnere tomme lister når person ikke har arbeidsforhold`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = emptyList(),
            statusCode = 200,
            errorMessage = null
        )

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        assertTrue(data.løpendeArbeidsforhold.isEmpty())
        assertTrue(data.historikk.isEmpty())
    }

    @Test
    fun `skal separere løpende og historiske arbeidsforhold korrekt`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        val løpendeArbeidsforhold = lagArbeidsforhold("999888777", sluttdato = null)
        val historiskArbeidsforhold = lagArbeidsforhold("888777666", sluttdato = LocalDate.of(2023, 12, 31))

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = listOf(løpendeArbeidsforhold, historiskArbeidsforhold),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", null)

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        assertEquals(1, data.løpendeArbeidsforhold.size)
        assertEquals(1, data.historikk.size)
        assertEquals("999888777", data.løpendeArbeidsforhold[0].organisasjonsnummer)
        assertEquals("888777666", data.historikk[0].organisasjonsnummer)
    }

    @Test
    fun `skal mappe arbeidsforhold med organisasjonsinformasjon fra Ereg`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        val arbeidsforhold = lagArbeidsforhold("999888777", sluttdato = null)

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = listOf(arbeidsforhold),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon("999888777") } returns lagEregRespons("999888777", "Test Bedrift AS")

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        assertEquals(1, data.løpendeArbeidsforhold.size)
        val arbeidsgiver = data.løpendeArbeidsforhold[0]
        assertEquals("Test Bedrift AS", arbeidsgiver.arbeidsgiver)
        assertEquals("999888777", arbeidsgiver.organisasjonsnummer)
    }

    @Test
    fun `skal håndtere ukjent organisasjon når Ereg ikke har data`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        val arbeidsforhold = lagArbeidsforhold("999888777", sluttdato = null)

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = listOf(arbeidsforhold),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", null)

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        assertEquals(1, data.løpendeArbeidsforhold.size)
        val arbeidsgiver = data.løpendeArbeidsforhold[0]
        assertEquals("999888777 - Ukjent navn", arbeidsgiver.arbeidsgiver)
    }

    @Test
    fun `skal mappe ansettelsesdetaljer korrekt`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        val arbeidsforhold = lagArbeidsforhold(
            orgnummer = "999888777",
            sluttdato = null,
            ansettelsesType = "Ordinær",
            stillingsprosent = 100.0,
            timerPrUke = 37.5,
            yrkeBeskrivelse = "Systemutvikler"
        )

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = listOf(arbeidsforhold),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", null)

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        assertEquals(1, data.løpendeArbeidsforhold.size)
        val detaljer = data.løpendeArbeidsforhold[0].ansettelsesDetaljer
        assertEquals(1, detaljer.size)
        assertEquals("Ordinær", detaljer[0].type)
        assertEquals(100.0, detaljer[0].stillingsprosent)
        assertEquals(37.5, detaljer[0].antallTimerPrUke)
        assertEquals("Systemutvikler", detaljer[0].yrke)
    }

    @Test
    fun `skal håndtere flere arbeidsforhold for samme person`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        val arbeidsforhold1 = lagArbeidsforhold("999888777", sluttdato = null)
        val arbeidsforhold2 = lagArbeidsforhold("888777666", sluttdato = null)
        val arbeidsforhold3 = lagArbeidsforhold("777666555", sluttdato = LocalDate.of(2023, 6, 30))

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = listOf(arbeidsforhold1, arbeidsforhold2, arbeidsforhold3),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", null)

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        assertEquals(2, data.løpendeArbeidsforhold.size)
        assertEquals(1, data.historikk.size)
    }

    @Test
    fun `skal deduplisere organisasjonsnumre ved Ereg-oppslag`() = runBlocking {
        val brukertilgangService = mockk<BrukertilgangService>()
        val aaregClient = mockk<AaregClient>()
        val eregClient = mockk<EregClient>()

        val arbeidsforhold1 = lagArbeidsforhold("999888777", sluttdato = null)
        val arbeidsforhold2 = lagArbeidsforhold("999888777", sluttdato = null)
        val arbeidsforhold3 = lagArbeidsforhold("999888777", sluttdato = LocalDate.of(2023, 12, 31))

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
        every { aaregClient.hentArbeidsforhold(any()) } returns AaregDataResultat(
            data = listOf(arbeidsforhold1, arbeidsforhold2, arbeidsforhold3),
            statusCode = 200,
            errorMessage = null
        )
        every { eregClient.hentOrganisasjon(any()) } returns lagEregRespons("999888777", null)

        val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
        val resultat = service.hentArbeidsforholdForPerson(PersonIdent("12345678901"))

        assertTrue(resultat is ArbeidsforholdResultat.Success)
        val data = (resultat as ArbeidsforholdResultat.Success).data
        assertEquals(2, data.løpendeArbeidsforhold.size)
        assertEquals(1, data.historikk.size)
        assertTrue(data.løpendeArbeidsforhold.all { it.organisasjonsnummer == "999888777" })
        assertTrue(data.historikk.all { it.organisasjonsnummer == "999888777" })
    }
}

// Hjelpefunksjoner for å lage testdata
private fun lagArbeidsforhold(
    orgnummer: String,
    sluttdato: LocalDate?,
    ansettelsesType: String = "Ordinær",
    stillingsprosent: Double? = 100.0,
    timerPrUke: Double? = 37.5,
    yrkeBeskrivelse: String = "Konsulent"
): Arbeidsforhold {
    return Arbeidsforhold(
        id = "test-id",
        type = Kodeverksentitet(kode = "NORMALT", beskrivelse = "Normalt arbeidsforhold"),
        arbeidstaker = Identer(
            identer = listOf(
                Ident(
                    type = Identtype.FOLKEREGISTERIDENT,
                    ident = "12345678901",
                    gjeldende = true
                )
            )
        ),
        arbeidssted = Arbeidssted(
            type = "Underenhet",
            identer = listOf(
                Ident(
                    type = Identtype.ORGANISASJONSNUMMER,
                    ident = orgnummer,
                    gjeldende = true
                )
            )
        ),
        opplysningspliktig = Opplysningspliktig(
            type = "Hovedenhet",
            identer = listOf(
                Ident(
                    type = Identtype.ORGANISASJONSNUMMER,
                    ident = orgnummer,
                    gjeldende = true
                )
            )
        ),
        ansettelsesperiode = Ansettelsesperiode(
            startdato = LocalDate.of(2020, 1, 1),
            sluttdato = sluttdato,
            sluttaarsak = null,
            varsling = null,
            sporingsinformasjon = null
        ),
        ansettelsesdetaljer = listOf(
            Ansettelsesdetaljer(
                type = ansettelsesType,
                arbeidstidsordning = null,
                ansettelsesform = null,
                yrke = Kodeverksentitet(kode = "1234", beskrivelse = yrkeBeskrivelse),
                antallTimerPrUke = timerPrUke,
                avtaltStillingsprosent = stillingsprosent,
                sisteStillingsprosentendring = null,
                sisteLoennsendring = null,
                rapporteringsmaaneder = Rapporteringsmaaneder(
                    fra = YearMonth.of(2020, 1),
                    til = null
                ),
                sporingsinformasjon = null
            )
        ),
        permisjoner = null,
        permitteringer = null,
        timerMedTimeloenn = null,
        idHistorikk = null,
        varsler = null,
        rapporteringsordning = Kodeverksentitet(kode = "A_ORDNINGEN", beskrivelse = "A-ordningen"),
        navArbeidsforholdId = 12345,
        navVersjon = 1,
        navUuid = "test-uuid",
        opprettet = LocalDateTime.now(),
        sistBekreftet = LocalDateTime.now(),
        bruksperiode = Bruksperiode(fom = null, tom = null),
        sporingsinformasjon = null,
        organisasjoner = emptyList()
    )
}

private fun lagEregRespons(orgnummer: String, navn: String?): EregRespons {
    return EregRespons(
        organisasjonsnummer = orgnummer,
        type = "AS",
        navn = if (navn != null) {
            no.nav.persondataapi.integrasjon.ereg.client.Navn(
                sammensattnavn = navn,
                navnelinje1 = navn,
                bruksperiode = no.nav.persondataapi.integrasjon.ereg.client.PeriodeTid(fom = LocalDateTime.now()),
                gyldighetsperiode = no.nav.persondataapi.integrasjon.ereg.client.PeriodeDato(fom = LocalDate.now())
            )
        } else null,
    )
}
