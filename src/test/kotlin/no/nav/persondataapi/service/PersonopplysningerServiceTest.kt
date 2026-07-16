package no.nav.persondataapi.service

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.generated.pdl.enums.AdressebeskyttelseGradering
import no.nav.persondataapi.generated.pdl.enums.ForelderBarnRelasjonRolle
import no.nav.persondataapi.generated.pdl.enums.Sivilstandstype
import no.nav.persondataapi.generated.pdl.hentperson.Foedselsdato
import no.nav.persondataapi.generated.pdl.hentperson.ForelderBarnRelasjon
import no.nav.persondataapi.generated.pdl.hentperson.Metadata
import no.nav.persondataapi.generated.pdl.hentperson.Navn
import no.nav.persondataapi.generated.pdl.hentperson.Person
import no.nav.persondataapi.generated.pdl.hentperson.Sivilstand
import no.nav.persondataapi.generated.pdl.hentperson.Statsborgerskap
import no.nav.persondataapi.generated.pdl.hentpersonbolk.HentPersonBolkResult
import no.nav.persondataapi.integrasjon.krr.client.KrrClient
import no.nav.persondataapi.integrasjon.krr.client.KrrDataResultat
import no.nav.persondataapi.integrasjon.norg2.client.NavLokalKontor
import no.nav.persondataapi.integrasjon.pdl.client.GeografiskTilknytningResultat
import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.integrasjon.pdl.client.PersonBolkResultat
import no.nav.persondataapi.integrasjon.pdl.client.PersonDataResultat
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.domene.PersonInformasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import no.nav.persondataapi.generated.pdl.hentpersonbolk.Adressebeskyttelse as BolkAdressebeskyttelse
import no.nav.persondataapi.generated.pdl.hentpersonbolk.Foedselsdato as BolkFoedselsdato
import no.nav.persondataapi.generated.pdl.hentpersonbolk.Navn as BolkNavn
import no.nav.persondataapi.generated.pdl.hentpersonbolk.Person as BolkPerson

class PersonopplysningerServiceTest {
    val brukertilgangService = mockk<BrukertilgangService>()
    val pdlClient = mockk<PdlClient>()
    val kodeverkService = mockk<KodeverkService>()
    val navTilhørigetService = mockk<NavTilhørighetService>()
    val krrClient = mockk<KrrClient>()

    private fun lagServiceMedStandardMocks(
        harTilgang: Boolean = true,
        personResultat: PersonDataResultat =
            PersonDataResultat(
                data = null,
                statusCode = 200,
                errorMessage = null,
            ),
        geoResultat: GeografiskTilknytningResultat =
            GeografiskTilknytningResultat(
                data = null,
                statusCode = 200,
                errorMessage = null,
            ),
        bolkResultat: PersonBolkResultat = PersonBolkResultat(data = emptyList(), statusCode = 200),
        lokalKontor: NavLokalKontor =
            NavLokalKontor(
                enhetId = 1,
                navn = "TestNav",
                enhetNr = "1",
                type = "LOKALKONTOR",
            ),
    ): PersonopplysningerService {
        clearAllMocks()

        every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns harTilgang
        coEvery { pdlClient.hentPerson(any()) } returns personResultat
        coEvery { pdlClient.hentGeografiskTilknytning(any()) } returns geoResultat
        coEvery { pdlClient.hentPersonBolk(any()) } returns bolkResultat
        coEvery { navTilhørigetService.finnLokalKontorForPersonIdent(any()) } returns lokalKontor
        every { krrClient.hentKontaktinformasjon(any()) } returns KrrDataResultat(epost = null)

        return PersonopplysningerService(
            pdlClient,
            brukertilgangService,
            kodeverkService,
            navTilhørigetService,
            krrClient,
        )
    }

    @Test
    fun `skal maskere data når saksbehandler ikke har tilgang`() =
        runBlocking {
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

            val service =
                lagServiceMedStandardMocks(
                    harTilgang = false,
                    personResultat =
                        PersonDataResultat(
                            data = person,
                            statusCode = 200,
                            errorMessage = null,
                        ),
                )

            every { kodeverkService.mapLandkodeTilLandnavn("NOR") } returns "Norge"

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
            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = null,
                            statusCode = 404,
                            errorMessage = "Not found",
                        ),
                )
            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.PersonIkkeFunnet)
        }

    @Test
    fun `skal returnere IngenTilgang når PdlClient returnerer 403`() =
        runBlocking {
            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = null,
                            statusCode = 403,
                            errorMessage = "Forbidden",
                        ),
                )
            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.IngenTilgang)
        }

    @Test
    fun `skal returnere FeilIBaksystem når PdlClient returnerer 500`() =
        runBlocking {
            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = null,
                            statusCode = 500,
                            errorMessage = "Internal server error",
                        ),
                )
            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.FeilIBaksystem)
        }

    @Test
    fun `skal returnere FeilIBaksystem når PdlClient returnerer annen feilkode`() =
        runBlocking {
            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = null,
                            statusCode = 502,
                            errorMessage = "Bad gateway",
                        ),
                )
            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.FeilIBaksystem)
        }

    @Test
    fun `skal returnere PersonIkkeFunnet når data er null selv med 200 status`() =
        runBlocking {
            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = null,
                            statusCode = 200,
                            errorMessage = null,
                        ),
                )
            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.PersonIkkeFunnet)
        }

    @Test
    fun `skal mappe personopplysninger med barn og ektefelle`() =
        runBlocking {
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

            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = person,
                            statusCode = 200,
                            errorMessage = null,
                        ),
                )

            every { kodeverkService.mapLandkodeTilLandnavn("NOR") } returns "Norge"

            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.Success)
            val data = (resultat as PersonopplysningerResultat.Success).data

            assertEquals("Ola", data.navn.fornavn)
            assertEquals("Nordmann", data.navn.mellomnavn)
            assertEquals("Testesen", data.navn.etternavn)
            assertEquals(2, data.familemedlemmer.size)
            assertEquals("BARN", data.familemedlemmer.firstOrNull { it.ident == "11111111111" }?.rolle)
            assertEquals("GIFT", data.familemedlemmer.firstOrNull { it.ident == "22222222222" }?.rolle)
            assertEquals(1, data.statsborgerskap.size)
            assertEquals("Norge", data.statsborgerskap[0])
        }

    @Test
    fun `skal håndtere person uten mellomnavn`() =
        runBlocking {
            val person =
                lagPerson(
                    fornavn = "Kari",
                    mellomnavn = null,
                    etternavn = "Normann",
                    foedselsdato = "1990-06-15",
                )
            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = person,
                            statusCode = 200,
                            errorMessage = null,
                        ),
                )
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
            val person =
                lagPerson(
                    fornavn = "Test",
                    etternavn = "Testesen",
                    foedselsdato = "2000-01-01",
                )
            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = person,
                            statusCode = 200,
                            errorMessage = null,
                        ),
                )
            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.Success)
            val data = (resultat as PersonopplysningerResultat.Success).data

            assertTrue(data.alder >= 24)
        }

    @Test
    fun `skal håndtere person med flere statsborgerskap`() =
        runBlocking {
            val person =
                lagPerson(
                    fornavn = "Multi",
                    etternavn = "Citizen",
                    foedselsdato = "1985-05-20",
                    statsborgerskap = listOf("NOR", "SWE", "DNK"),
                )
            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = person,
                            statusCode = 200,
                            errorMessage = null,
                        ),
                )

            every { kodeverkService.mapLandkodeTilLandnavn("NOR") } returns "Norge"
            every { kodeverkService.mapLandkodeTilLandnavn("SWE") } returns "Sverige"
            every { kodeverkService.mapLandkodeTilLandnavn("DNK") } returns "Danmark"

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
            val person =
                lagPerson(
                    fornavn = "Unknown",
                    etternavn = "Country",
                    foedselsdato = "1980-03-10",
                    statsborgerskap = listOf("XXX"),
                )
            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = person,
                            statusCode = 200,
                            errorMessage = null,
                        ),
                )
            every { kodeverkService.mapLandkodeTilLandnavn("XXX") } returns "Ukjent"

            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.Success)
            val data = (resultat as PersonopplysningerResultat.Success).data

            assertEquals(1, data.statsborgerskap.size)
            assertEquals("Ukjent", data.statsborgerskap[0])
        }

    @Test
    fun `skal håndtere sivilstand uten relatert person`() =
        runBlocking {
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
            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat =
                        PersonDataResultat(
                            data = person,
                            statusCode = 200,
                            errorMessage = null,
                        ),
                )
            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.Success)
            val data = (resultat as PersonopplysningerResultat.Success).data

            // Familiemedlemmer skal være tom når sivilstand ikke har relatert person
            assertTrue(data.familemedlemmer.isEmpty())
            assertNotNull(data.sivilstand)
        }

    @Test
    fun `skal berike familiemedlemmer med navn fra hentPersonBolk`() =
        runBlocking {
            val person =
                lagPerson(
                    fornavn = "Ola",
                    etternavn = "Testesen",
                    foedselsdato = "2000-01-01",
                    forelderBarnRelasjon =
                        listOf(
                            lagForelderBarnRelasjon("11111111111", ForelderBarnRelasjonRolle.BARN),
                        ),
                )

            val bolkResultat =
                PersonBolkResultat(
                    statusCode = 200,
                    data =
                        listOf(
                            lagBolkResultat(
                                ident = "11111111111",
                                fornavn = "Barn",
                                etternavn = "Testesen",
                                foedselsdato = "2015-03-10",
                            ),
                        ),
                )

            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat = PersonDataResultat(data = person, statusCode = 200, errorMessage = null),
                    bolkResultat = bolkResultat,
                )

            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.Success)
            val data = (resultat as PersonopplysningerResultat.Success).data

            val barn = data.familemedlemmer.firstOrNull { it.ident == "11111111111" }
            assertNotNull(barn)
            assertEquals("Barn", barn?.fornavn)
            assertEquals("Testesen", barn?.etternavn)
            assertEquals("2015-03-10", barn?.fødselsdato)
        }

    @Test
    fun `skal returnere familiemedlem med null-navn når bolk-kode ikke er ok`() =
        runBlocking {
            val person =
                lagPerson(
                    fornavn = "Ola",
                    etternavn = "Testesen",
                    foedselsdato = "2000-01-01",
                    forelderBarnRelasjon =
                        listOf(
                            lagForelderBarnRelasjon("11111111111", ForelderBarnRelasjonRolle.BARN),
                        ),
                )

            val bolkResultat =
                PersonBolkResultat(
                    statusCode = 200,
                    data =
                        listOf(
                            HentPersonBolkResult(ident = "11111111111", code = "not-found", person = null),
                        ),
                )

            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat = PersonDataResultat(data = person, statusCode = 200, errorMessage = null),
                    bolkResultat = bolkResultat,
                )

            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.Success)
            val data = (resultat as PersonopplysningerResultat.Success).data

            val barn = data.familemedlemmer.firstOrNull { it.ident == "11111111111" }
            assertNotNull(barn)
            assertNull(barn?.fornavn)
            assertNull(barn?.etternavn)
        }

    @Test
    fun `skal sette adressebeskyttelse FORTROLIG på familiemedlem fra bolk`() =
        runBlocking {
            val person =
                lagPerson(
                    fornavn = "Ola",
                    etternavn = "Testesen",
                    foedselsdato = "2000-01-01",
                    forelderBarnRelasjon =
                        listOf(
                            lagForelderBarnRelasjon("11111111111", ForelderBarnRelasjonRolle.BARN),
                        ),
                )

            val bolkResultat =
                PersonBolkResultat(
                    statusCode = 200,
                    data =
                        listOf(
                            lagBolkResultat(
                                ident = "11111111111",
                                fornavn = "Skjult",
                                etternavn = "Person",
                                gradering = AdressebeskyttelseGradering.FORTROLIG,
                            ),
                        ),
                )

            val service =
                lagServiceMedStandardMocks(
                    harTilgang = true,
                    personResultat = PersonDataResultat(data = person, statusCode = 200, errorMessage = null),
                    bolkResultat = bolkResultat,
                )

            val resultat = service.hentPersonopplysningerForPerson(PersonIdent("12345678901"))

            assertTrue(resultat is PersonopplysningerResultat.Success)
            val data = (resultat as PersonopplysningerResultat.Success).data

            val barn = data.familemedlemmer.firstOrNull { it.ident == "11111111111" }
            assertNotNull(barn)
            assertEquals(PersonInformasjon.Skjerming.FORTROLIG, barn?.adressebeskyttelse)
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
                no.nav.persondataapi.generated.pdl.hentperson.Bostedsadresse(
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

private fun lagBolkResultat(
    ident: String,
    fornavn: String,
    etternavn: String,
    mellomnavn: String? = null,
    foedselsdato: String? = null,
    gradering: AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
): HentPersonBolkResult =
    HentPersonBolkResult(
        ident = ident,
        code = "ok",
        person =
            BolkPerson(
                navn = listOf(BolkNavn(fornavn = fornavn, mellomnavn = mellomnavn, etternavn = etternavn)),
                foedselsdato = listOf(BolkFoedselsdato(foedselsdato = foedselsdato)),
                adressebeskyttelse = listOf(BolkAdressebeskyttelse(gradering = gradering)),
            ),
    )
