package no.nav.persondataapi.adressebeskyttelse


import no.nav.persondataapi.generated.enums.AdressebeskyttelseGradering
import no.nav.persondataapi.generated.hentperson.Adressebeskyttelse
import no.nav.persondataapi.generated.hentperson.Folkeregistermetadata
import no.nav.persondataapi.generated.hentperson.Metadata
import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.rest.domene.PersonInformasjon
import no.nav.persondataapi.service.nåværendeAdresseBeskyttelse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AdresseBeskyttelsesTest {

    @Test
    fun `dersom ingen adressebeskyttelse så skal UGRADERT returneres`() {
        val person = lagPdlPerson(emptyList())
        Assertions.assertEquals(PersonInformasjon.Skjerming.UGRADERT,person.nåværendeAdresseBeskyttelse())
    }
    @Test
    fun `dersom  adressebeskyttelse som er utgått så skal ÅPEN returneres`() {

        val person = lagPdlPerson(
            adressebeskyttelse = (
                    listOf<Adressebeskyttelse>(
                        lagAdressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,true)
                    )
            )

        )
        Assertions.assertEquals(PersonInformasjon.Skjerming.UGRADERT,person.nåværendeAdresseBeskyttelse())
    }
    @Test
    fun `dersom adressebeskyttelse  ikke er utgått så skal Ikke ÅPEN returneres`() {

        val person = lagPdlPerson(
            adressebeskyttelse = (
                    listOf<Adressebeskyttelse>(
                        lagAdressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,false)
                    )
                    )

        )
        Assertions.assertEquals(PersonInformasjon.Skjerming.STRENGT_FORTROLIG_UTLAND,person.nåværendeAdresseBeskyttelse())
    }
}

fun lagPdlPerson(adressebeskyttelse : List<Adressebeskyttelse>): Person {
    return Person(
        falskIdentitet = null,
        bostedsadresse = emptyList(),
        oppholdsadresse = emptyList(),
        deltBosted = emptyList(),
        forelderBarnRelasjon = emptyList(),
        kontaktadresse = emptyList(),
        kontaktinformasjonForDoedsbo = emptyList(),
        utenlandskIdentifikasjonsnummer = emptyList(),
        adressebeskyttelse = adressebeskyttelse,
        foedested = emptyList(),
        foedselsdato = emptyList(),
        doedsfall = emptyList(),
        kjoenn = emptyList(),
        navn = emptyList(),
        folkeregisterpersonstatus = emptyList(),
        identitetsgrunnlag = emptyList(),
        tilrettelagtKommunikasjon = emptyList(),
        folkeregisteridentifikator = emptyList(),
        navspersonidentifikator = emptyList(),
        statsborgerskap = emptyList(),
        sikkerhetstiltak = emptyList(),
        opphold = emptyList(),
        sivilstand = emptyList(),
        telefonnummer = emptyList(),
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = emptyList(),
        vergemaalEllerFremtidsfullmakt = emptyList(),
        foreldreansvar = emptyList(),
        rettsligHandleevne = emptyList(),
        doedfoedtBarn = emptyList()
    )
}

fun lagAdressebeskyttelse(adressebeskyttelseGradering: AdressebeskyttelseGradering,historisk: Boolean) : Adressebeskyttelse {
    return Adressebeskyttelse(
        gradering = adressebeskyttelseGradering,
        folkeregistermetadata = Folkeregistermetadata(
            opphoerstidspunkt = "2020-01-01",
            aarsak = null,
            ajourholdstidspunkt = "2020-01-01",
            gyldighetstidspunkt = "2020-01-01",
            kilde = "FREG",
            sekvens = 1
        ),
        metadata = Metadata(
            endringer = emptyList(),
            "FREG",
            null,
            historisk
        )
    )
}