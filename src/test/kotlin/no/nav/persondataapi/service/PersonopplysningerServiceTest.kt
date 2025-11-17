package no.nav.persondataapi.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.generated.enums.ForelderBarnRelasjonRolle
import no.nav.persondataapi.generated.enums.Sivilstandstype
import no.nav.persondataapi.generated.hentperson.Foedselsdato
import no.nav.persondataapi.generated.hentperson.ForelderBarnRelasjon
import no.nav.persondataapi.generated.hentperson.Metadata
import no.nav.persondataapi.generated.hentperson.Navn
import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.generated.hentperson.Sivilstand
import no.nav.persondataapi.generated.hentperson.Statsborgerskap
import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.integrasjon.pdl.client.PersonDataResultat
import no.nav.persondataapi.rest.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PersonopplysningerServiceTest {
	@Test
	fun `skal maskere data når saksbehandler ikke har tilgang`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			val person =
				lagPerson(
					fornavn = "Ola",
					mellomnavn = "Nordmann",
					etternavn = "Testesen",
					foedselsdato = "2000-01-01",
					statsborgerskap = listOf("NOR"),
					forelderBarnRelasjon =
						listOf(
							lagForelderBarnRelasjon("11111111111", ForelderBarnRelasjonRolle.BARN),
						),
					sivilstand =
						listOf(
							lagSivilstand("22222222222", Sivilstandstype.GIFT),
						),
				)

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns false
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = person,
					statusCode = 200,
					errorMessage = null,
				)
			every { kodeverkService.mapLandkodeTilLandnavn("NOR") } returns "Norge"

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.Success)
			val data = (resultat as PersonopplysningerResultat.Success).data
			// Data skal være maskert - @Maskert-felter er erstattet med *******
			assertEquals("*******", data.navn.fornavn)
			assertEquals("*******", data.navn.mellomnavn)
			assertEquals("*******", data.navn.etternavn)
		}

	@Test
	fun `skal returnere PersonIkkeFunnet når PdlClient returnerer 404`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = null,
					statusCode = 404,
					errorMessage = "Not found",
				)

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.PersonIkkeFunnet)
		}

	@Test
	fun `skal returnere IngenTilgang når PdlClient returnerer 403`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = null,
					statusCode = 403,
					errorMessage = "Forbidden",
				)

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.IngenTilgang)
		}

	@Test
	fun `skal returnere FeilIBaksystem når PdlClient returnerer 500`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = null,
					statusCode = 500,
					errorMessage = "Internal server error",
				)

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.FeilIBaksystem)
		}

	@Test
	fun `skal returnere FeilIBaksystem når PdlClient returnerer annen feilkode`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = null,
					statusCode = 502,
					errorMessage = "Bad gateway",
				)

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.FeilIBaksystem)
		}

	@Test
	fun `skal returnere PersonIkkeFunnet når data er null selv med 200 status`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = null,
					statusCode = 200,
					errorMessage = null,
				)

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.PersonIkkeFunnet)
		}

	@Test
	fun `skal mappe personopplysninger med barn og ektefelle`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			val person =
				lagPerson(
					fornavn = "Ola",
					mellomnavn = "Nordmann",
					etternavn = "Testesen",
					foedselsdato = "2000-01-01",
					statsborgerskap = listOf("NOR"),
					forelderBarnRelasjon =
						listOf(
							lagForelderBarnRelasjon("11111111111", ForelderBarnRelasjonRolle.BARN),
						),
					sivilstand =
						listOf(
							lagSivilstand("22222222222", Sivilstandstype.GIFT),
						),
				)

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = person,
					statusCode = 200,
					errorMessage = null,
				)
			every { kodeverkService.mapLandkodeTilLandnavn("NOR") } returns "Norge"

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.Success)
			val data = (resultat as PersonopplysningerResultat.Success).data

			assertEquals("Ola", data.navn.fornavn)
			assertEquals("Nordmann", data.navn.mellomnavn)
			assertEquals("Testesen", data.navn.etternavn)
			assertEquals(2, data.familemedlemmer.size)
			assertEquals("BARN", data.familemedlemmer["11111111111"])
			assertEquals("GIFT", data.familemedlemmer["22222222222"])
			assertEquals(1, data.statsborgerskap.size)
			assertEquals("Norge", data.statsborgerskap[0])
		}

	@Test
	fun `skal håndtere person uten mellomnavn`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			val person =
				lagPerson(
					fornavn = "Kari",
					mellomnavn = null,
					etternavn = "Normann",
					foedselsdato = "1990-06-15",
				)

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = person,
					statusCode = 200,
					errorMessage = null,
				)

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.Success)
			val data = (resultat as PersonopplysningerResultat.Success).data

			assertEquals("Kari", data.navn.fornavn)
			assertNull(data.navn.mellomnavn)
			assertEquals("Normann", data.navn.etternavn)
		}

	@Test
	fun `skal beregne korrekt alder basert på fødselsdato`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			val person =
				lagPerson(
					fornavn = "Test",
					etternavn = "Testesen",
					foedselsdato = "2000-01-01",
				)

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = person,
					statusCode = 200,
					errorMessage = null,
				)

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.Success)
			val data = (resultat as PersonopplysningerResultat.Success).data

			assertTrue(data.alder >= 24)
		}

	@Test
	fun `skal håndtere person med flere statsborgerskap`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			val person =
				lagPerson(
					fornavn = "Multi",
					etternavn = "Citizen",
					foedselsdato = "1985-05-20",
					statsborgerskap = listOf("NOR", "SWE", "DNK"),
				)

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = person,
					statusCode = 200,
					errorMessage = null,
				)
			every { kodeverkService.mapLandkodeTilLandnavn("NOR") } returns "Norge"
			every { kodeverkService.mapLandkodeTilLandnavn("SWE") } returns "Sverige"
			every { kodeverkService.mapLandkodeTilLandnavn("DNK") } returns "Danmark"

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.Success)
			val data = (resultat as PersonopplysningerResultat.Success).data

			assertEquals(3, data.statsborgerskap.size)
			assertTrue(data.statsborgerskap.contains("Norge"))
			assertTrue(data.statsborgerskap.contains("Sverige"))
			assertTrue(data.statsborgerskap.contains("Danmark"))
		}

	@Test
	fun `skal håndtere ukjent landkode ved bruk av kodeverk`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			val person =
				lagPerson(
					fornavn = "Unknown",
					etternavn = "Country",
					foedselsdato = "1980-03-10",
					statsborgerskap = listOf("XXX"),
				)

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = person,
					statusCode = 200,
					errorMessage = null,
				)
			every { kodeverkService.mapLandkodeTilLandnavn("XXX") } returns "Ukjent"

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.Success)
			val data = (resultat as PersonopplysningerResultat.Success).data

			assertEquals(1, data.statsborgerskap.size)
			assertEquals("Ukjent", data.statsborgerskap[0])
		}

	@Test
	fun `skal håndtere sivilstand uten relatert person`() =
		runBlocking {
			val brukertilgangService = mockk<BrukertilgangService>()
			val pdlClient = mockk<PdlClient>()
			val kodeverkService = mockk<KodeverkService>()

			val person =
				lagPerson(
					fornavn = "Single",
					etternavn = "Person",
					foedselsdato = "1995-12-25",
					sivilstand =
						listOf(
							lagSivilstand(null, Sivilstandstype.UGIFT),
						),
				)

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			coEvery { pdlClient.hentPerson(any()) } returns
				PersonDataResultat(
					data = person,
					statusCode = 200,
					errorMessage = null,
				)

			val service = PersonopplysningerService(pdlClient, brukertilgangService, kodeverkService)
			val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

			assertTrue(resultat is PersonopplysningerResultat.Success)
			val data = (resultat as PersonopplysningerResultat.Success).data

			// Familiemedlemmer skal være tom når sivilstand ikke har relatert person
			assertTrue(data.familemedlemmer.isEmpty())
			assertNotNull(data.sivilstand)
		}
}

// Hjelpefunksjoner for å lage testdata
private fun lagPerson(
	fornavn: String,
	etternavn: String,
	mellomnavn: String? = null,
	foedselsdato: String,
	statsborgerskap: List<String> = emptyList(),
	forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
	sivilstand: List<Sivilstand> = emptyList(),
): Person {
	val metadata = Metadata(endringer = emptyList(), master = "Freg", opplysningsId = "test", historisk = false)

	// Provide default sivilstand if none specified
	val actualSivilstand =
		if (sivilstand.isEmpty()) {
			listOf(lagSivilstand(null, Sivilstandstype.UGIFT))
		} else {
			sivilstand
		}

	return Person(
		navn =
			listOf(
				Navn(
					fornavn = fornavn,
					mellomnavn = mellomnavn,
					etternavn = etternavn,
					metadata = metadata,
					folkeregistermetadata = null,
				),
			),
		foedselsdato =
			listOf(
				Foedselsdato(
					foedselsdato = foedselsdato,
					metadata = metadata,
					folkeregistermetadata = null,
				),
			),
		statsborgerskap =
			statsborgerskap.map {
				Statsborgerskap(
					land = it,
					metadata = metadata,
					folkeregistermetadata = null,
				)
			},
		forelderBarnRelasjon = forelderBarnRelasjon,
		sivilstand = actualSivilstand,
		bostedsadresse =
			listOf(
				no.nav.persondataapi.generated.hentperson.Bostedsadresse(
					vegadresse = null,
					matrikkeladresse = null,
					utenlandskAdresse = null,
					ukjentBosted = null,
					metadata = metadata,
					folkeregistermetadata = null,
				),
			),
		oppholdsadresse = emptyList(),
		deltBosted = emptyList(),
		kontaktadresse = emptyList(),
		kontaktinformasjonForDoedsbo = emptyList(),
		utenlandskIdentifikasjonsnummer = emptyList(),
		adressebeskyttelse = emptyList(),
		foedested = emptyList(),
		doedsfall = emptyList(),
		kjoenn = emptyList(),
		folkeregisterpersonstatus = emptyList(),
		identitetsgrunnlag = emptyList(),
		tilrettelagtKommunikasjon = emptyList(),
		folkeregisteridentifikator = emptyList(),
		navspersonidentifikator = emptyList(),
		sikkerhetstiltak = emptyList(),
		opphold = emptyList(),
		telefonnummer = emptyList(),
		innflyttingTilNorge = emptyList(),
		utflyttingFraNorge = emptyList(),
		vergemaalEllerFremtidsfullmakt = emptyList(),
		foreldreansvar = emptyList(),
		rettsligHandleevne = emptyList(),
		doedfoedtBarn = emptyList(),
		falskIdentitet = null,
	)
}

private fun lagForelderBarnRelasjon(
	relatertPersonIdent: String,
	rolle: ForelderBarnRelasjonRolle,
): ForelderBarnRelasjon {
	val metadata = Metadata(endringer = emptyList(), master = "Freg", opplysningsId = "test", historisk = false)

	return ForelderBarnRelasjon(
		relatertPersonsIdent = relatertPersonIdent,
		relatertPersonsRolle = rolle,
		minRolleForPerson = null,
		metadata = metadata,
		folkeregistermetadata = null,
	)
}

private fun lagSivilstand(
	relatertVedSivilstand: String?,
	type: Sivilstandstype,
): Sivilstand {
	val metadata = Metadata(endringer = emptyList(), master = "Freg", opplysningsId = "test", historisk = false)

	return Sivilstand(
		type = type,
		gyldigFraOgMed = null,
		relatertVedSivilstand = relatertVedSivilstand,
		bekreftelsesdato = null,
		metadata = metadata,
		folkeregistermetadata = null,
	)
}
