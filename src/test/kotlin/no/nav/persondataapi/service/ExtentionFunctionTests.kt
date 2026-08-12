package no.nav.persondataapi.service

import no.nav.inntekt.generated.model.HistorikkData
import no.nav.inntekt.generated.model.Inntektsinformasjon
import no.nav.inntekt.generated.model.Loennsinntekt
import no.nav.inntekt.generated.model.YtelseFraOffentlige
import no.nav.persondataapi.generated.pdl.hentperson.Bostedsadresse
import no.nav.persondataapi.generated.pdl.hentperson.Folkeregisteridentifikator
import no.nav.persondataapi.generated.pdl.hentperson.Folkeregistermetadata
import no.nav.persondataapi.generated.pdl.hentperson.Metadata
import no.nav.persondataapi.generated.pdl.hentperson.Telefonnummer
import no.nav.persondataapi.generated.pdl.hentperson.Vegadresse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

private val standardMetadata =
    Metadata(endringer = emptyList(), master = "Freg", opplysningsId = "test", historisk = false)
private val historiskMetadata = standardMetadata.copy(historisk = true)

private fun lagVegadresse(adressenavn: String) =
    Vegadresse(
        matrikkelId = null,
        husbokstav = null,
        husnummer = "1",
        adressenavn = adressenavn,
        bruksenhetsnummer = null,
        tilleggsnavn = null,
        postnummer = "0001",
        kommunenummer = "0301",
        bydelsnummer = null,
        koordinater = null,
    )

private fun lagBostedsadresse(
    adressenavn: String,
    historisk: Boolean,
    gyldigFraOgMed: String? = null,
    gyldigTilOgMed: String? = null,
) = Bostedsadresse(
    angittFlyttedato = null,
    coAdressenavn = null,
    gyldigFraOgMed = gyldigFraOgMed,
    gyldigTilOgMed = gyldigTilOgMed,
    vegadresse = lagVegadresse(adressenavn),
    matrikkeladresse = null,
    ukjentBosted = null,
    utenlandskAdresse = null,
    folkeregistermetadata = null,
    metadata = if (historisk) historiskMetadata else standardMetadata,
)

private fun lagTelefonnummer(
    nummer: String,
    prioritet: Int,
    historisk: Boolean = false,
) = Telefonnummer(
    landskode = "+47",
    nummer = nummer,
    prioritet = prioritet,
    metadata = if (historisk) historiskMetadata else standardMetadata,
)

class ExtentionFunctionTests {
    @Test
    fun `nåværendeBostedsadresse skal filtrere bort historiske adresser`() {
        val person =
            lagPersonMedAdresserOgTelefon(
                bostedsadresser =
                    listOf(
                        lagBostedsadresse("Gammelveien", historisk = true),
                        lagBostedsadresse("Nyveien", historisk = false),
                    ),
            )

        val adresse = person.nåværendeBostedsadresse()

        assertEquals("Nyveien", adresse?.norskAdresse?.adressenavn)
    }

    @Test
    fun `nåværendeBostedsadresse skal returnere null når kun historiske adresser finnes`() {
        val person =
            lagPersonMedAdresserOgTelefon(
                bostedsadresser = listOf(lagBostedsadresse("Gammelveien", historisk = true)),
            )

        assertNull(person.nåværendeBostedsadresse())
    }

    @Test
    fun `telefonnummer skal filtrere bort historiske numre og sortere på prioritet`() {
        val person =
            lagPersonMedAdresserOgTelefon(
                telefonnumre =
                    listOf(
                        lagTelefonnummer("22222222", prioritet = 2),
                        lagTelefonnummer("11111111", prioritet = 1),
                        lagTelefonnummer("99999999", prioritet = 0, historisk = true),
                    ),
            )

        val telefonnumre = person.telefonnummer()

        assertEquals(2, telefonnumre.size)
        assertEquals("11111111", telefonnumre[0].nummer)
        assertEquals("22222222", telefonnumre[1].nummer)
    }

    @Test
    fun `adresseHistorikkSiste5År skal ekskludere adresser eldre enn 5 år`() {
        val forGammel = LocalDate.now().minusYears(6).toString()
        val innenfor = LocalDate.now().minusYears(2).toString()

        val person =
            lagPersonMedAdresserOgTelefon(
                bostedsadresser =
                    listOf(
                        lagBostedsadresse("Gammelveien", historisk = true, gyldigTilOgMed = forGammel),
                        lagBostedsadresse("Nyligveien", historisk = true, gyldigTilOgMed = innenfor),
                        lagBostedsadresse("Nyveien", historisk = false),
                    ),
            )

        val historikk = person.adresseHistorikkSiste5År()

        assertEquals(2, historikk.size)
        assertTrue(historikk.any { it.adresse.norskAdresse?.adressenavn == "Nyligveien" })
        assertTrue(historikk.any { it.adresse.norskAdresse?.adressenavn == "Nyveien" })
    }

    @Test
    fun historikkDataSkalHåndtereNullVersjoner() {
        val historikkData =
            HistorikkData(
                maaned = "2020-01-01",
                opplysningspliktig = "123",
                underenhet = "1234",
                norskident = "12345678901",
                versjoner = emptyList(),
            )
        Assertions.assertFalse(historikkData.harHistorikkPåNormallønn())
    }

    @Test
    fun kunYtelseFraOffentligeSkalIkkeTelleIHistorikken() {
        val historikkData =
            HistorikkData(
                maaned = "2020-01-01",
                opplysningspliktig = "123",
                underenhet = "1234",
                norskident = "12345678901",
                versjoner =
                    listOf(
                        Inntektsinformasjon(
                            maaned = "1234",
                            opplysningspliktig = "1234",
                            underenhet = "1234",
                            norskident = "12345678901",
                            oppsummeringstidspunkt = OffsetDateTime.now().minusDays(30),
                            inntektListe =
                                listOf(
                                    YtelseFraOffentlige(
                                        beloep = BigDecimal.valueOf(12344),
                                        fordel = "kontantytelse",
                                        beskrivelse = "",
                                        inngaarIGrunnlagForTrekk = false,
                                        utloeserArbeidsgiveravgift = false,
                                        type = "YtelseFraOffentlige",
                                    ),
                                ),
                        ),
                        Inntektsinformasjon(
                            maaned = "1234",
                            opplysningspliktig = "1234",
                            underenhet = "1234",
                            norskident = "12345678901",
                            oppsummeringstidspunkt = OffsetDateTime.now().minusDays(10),
                            inntektListe =
                                listOf(
                                    YtelseFraOffentlige(
                                        beloep = BigDecimal.valueOf(12344),
                                        fordel = "kontantytelse",
                                        beskrivelse = "",
                                        inngaarIGrunnlagForTrekk = false,
                                        utloeserArbeidsgiveravgift = false,
                                        type = "YtelseFraOffentlige",
                                    ),
                                ),
                        ),
                    ),
            )
        Assertions.assertFalse(historikkData.harHistorikkPåNormallønn())
    }

    @Test
    fun kunEttInnslagAvLonnsInntektSkalIkkeTelleIHistorikken() {
        val historikkData =
            HistorikkData(
                maaned = "2020-01-01",
                opplysningspliktig = "123",
                underenhet = "1234",
                norskident = "12345678901",
                versjoner =
                    listOf(
                        Inntektsinformasjon(
                            maaned = "1234",
                            opplysningspliktig = "1234",
                            underenhet = "1234",
                            norskident = "12345678901",
                            oppsummeringstidspunkt = OffsetDateTime.now().minusDays(30),
                            inntektListe =
                                listOf(
                                    YtelseFraOffentlige(
                                        beloep = BigDecimal.valueOf(12344),
                                        fordel = "kontantytelse",
                                        beskrivelse = "",
                                        inngaarIGrunnlagForTrekk = false,
                                        utloeserArbeidsgiveravgift = false,
                                        type = "YtelseFraOffentlige",
                                    ),
                                ),
                        ),
                        Inntektsinformasjon(
                            maaned = "1234",
                            opplysningspliktig = "1234",
                            underenhet = "1234",
                            norskident = "12345678901",
                            oppsummeringstidspunkt = OffsetDateTime.now().minusDays(10),
                            inntektListe =
                                listOf(
                                    Loennsinntekt(
                                        beloep = BigDecimal.valueOf(12344),
                                        fordel = "kontantytelse",
                                        beskrivelse = "",
                                        inngaarIGrunnlagForTrekk = false,
                                        utloeserArbeidsgiveravgift = false,
                                        type = "YtelseFraOffentlige",
                                    ),
                                ),
                        ),
                    ),
            )
        Assertions.assertFalse(historikkData.harHistorikkPåNormallønn())
    }

    @Test
    fun merEnEttInnslagAvLonnsInntektSkalIkkeTelleIHistorikken() {        val historikkData =
            HistorikkData(
                maaned = "2020-01-01",
                opplysningspliktig = "123",
                underenhet = "1234",
                norskident = "12345678901",
                versjoner =
                    listOf(
                        Inntektsinformasjon(
                            maaned = "1234",
                            opplysningspliktig = "1234",
                            underenhet = "1234",
                            norskident = "12345678901",
                            oppsummeringstidspunkt = OffsetDateTime.now().minusDays(30),
                            inntektListe =
                                listOf(
                                    Loennsinntekt(
                                        beloep = BigDecimal.valueOf(12344),
                                        fordel = "kontantytelse",
                                        beskrivelse = "",
                                        inngaarIGrunnlagForTrekk = false,
                                        utloeserArbeidsgiveravgift = false,
                                        type = "YtelseFraOffentlige",
                                    ),
                                ),
                        ),
                        Inntektsinformasjon(
                            maaned = "1234",
                            opplysningspliktig = "1234",
                            underenhet = "1234",
                            norskident = "12345678901",
                            oppsummeringstidspunkt = OffsetDateTime.now().minusDays(10),
                            inntektListe =
                                listOf(
                                    Loennsinntekt(
                                        beloep = BigDecimal.valueOf(12344),
                                        fordel = "kontantytelse",
                                        beskrivelse = "",
                                        inngaarIGrunnlagForTrekk = false,
                                        utloeserArbeidsgiveravgift = false,
                                        type = "YtelseFraOffentlige",
                                    ),
                                ),
                        ),
                    ),
            )
        Assertions.assertTrue(historikkData.harHistorikkPåNormallønn())
    }

    @Test
    fun `folkeregisterIdenter skal returnere gjeldende og historiske identer`() {
        val person =
            lagPersonMedAdresserOgTelefon(
                folkeregisteridentifikatorer =
                    listOf(
                        lagFolkeregisteridentifikator("12345678901", historisk = false),
                        lagFolkeregisteridentifikator("09876543210", historisk = true),
                    ),
            )

        val identer = person.folkeregisterIdenter()

        assertEquals(2, identer.size)
        val gjeldende = identer.first { !it.historisk }
        assertEquals("12345678901", gjeldende.personIdent)
        assertEquals("FOEDSELSNUMMER", gjeldende.type)
        val historisk = identer.first { it.historisk }
        assertEquals("09876543210", historisk.personIdent)
    }

    @Test
    fun `folkeregisterIdenter skal returnere tom liste når ingen identer finnes`() {
        val person = lagPersonMedAdresserOgTelefon()

        val identer = person.folkeregisterIdenter()

        assertTrue(identer.isEmpty())
    }

    @Test
    fun `folkeregisterIdenter skal bevare type fra PDL`() {
        val person =
            lagPersonMedAdresserOgTelefon(
                folkeregisteridentifikatorer =
                    listOf(
                        lagFolkeregisteridentifikator("12345678901", type = "FOEDSELSNUMMER"),
                        lagFolkeregisteridentifikator("99999900001", type = "DNR", historisk = true),
                    ),
            )

        val identer = person.folkeregisterIdenter()

        assertEquals("FOEDSELSNUMMER", identer.first { !it.historisk }.type)
        assertEquals("DNR", identer.first { it.historisk }.type)
    }
}

private fun lagPersonMedAdresserOgTelefon(
    bostedsadresser: List<Bostedsadresse> = emptyList(),
    telefonnumre: List<Telefonnummer> = emptyList(),
    folkeregisteridentifikatorer: List<Folkeregisteridentifikator> = emptyList(),
): no.nav.persondataapi.generated.pdl.hentperson.Person =
    no.nav.persondataapi.generated.pdl.hentperson.Person(
        navn = emptyList(),
        foedselsdato = emptyList(),
        statsborgerskap = emptyList(),
        forelderBarnRelasjon = emptyList(),
        sivilstand = emptyList(),
        bostedsadresse = bostedsadresser,
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
        folkeregisteridentifikator = folkeregisteridentifikatorer,
        navspersonidentifikator = emptyList(),
        sikkerhetstiltak = emptyList(),
        opphold = emptyList(),
        telefonnummer = telefonnumre,
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = emptyList(),
        vergemaalEllerFremtidsfullmakt = emptyList(),
        foreldreansvar = emptyList(),
        rettsligHandleevne = emptyList(),
        doedfoedtBarn = emptyList(),
        falskIdentitet = null,
    )

private val tomFolkeregistermetadata =
    Folkeregistermetadata(
        aarsak = null,
        ajourholdstidspunkt = null,
        gyldighetstidspunkt = null,
        kilde = null,
        opphoerstidspunkt = null,
        sekvens = null,
    )

private fun lagFolkeregisteridentifikator(
    identifikasjonsnummer: String,
    type: String = "FOEDSELSNUMMER",
    historisk: Boolean = false,
) = Folkeregisteridentifikator(
    identifikasjonsnummer = identifikasjonsnummer,
    status = "I_BRUK",
    type = type,
    folkeregistermetadata = tomFolkeregistermetadata,
    metadata = if (historisk) historiskMetadata else standardMetadata,
)
