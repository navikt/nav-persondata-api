package no.nav.persondataapi.rest.oppslag

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class MaskeringUtilTest {
	// Test data classes
	data class EnkelPerson(
		@Maskert
		val navn: String,
		val alder: Int,
	)

	data class PersonMedCustomMaskering(
		@Maskert(maskertVerdi = "SKJULT")
		val navn: String,
		val epost: String,
	)

	data class PersonUtenMaskering(
		val navn: String,
		val adresse: String,
	)

	data class NestedPerson(
		@Maskert
		val navn: String,
		val kontaktinfo: Kontaktinfo,
	)

	data class Kontaktinfo(
		@Maskert
		val telefon: String,
		val epost: String,
	)

	data class PersonMedListe(
		@Maskert
		val navn: String,
		val adresser: List<Adresse>,
	)

	data class Adresse(
		@Maskert
		val gate: String,
		val postnummer: String,
	)

	enum class Status {
		AKTIV,
		INAKTIV,
	}

	data class PersonMedEnum(
		@Maskert
		val navn: String,
		val status: Status,
	)

	@Test
	fun `skal maskere felt med Maskert-annotasjon med standard verdi`() {
		val person = EnkelPerson(navn = "Ola Nordmann", alder = 30)

		val maskertPerson = maskerObjekt(person)

		assertEquals("*******", maskertPerson.navn)
		assertEquals(30, maskertPerson.alder)
	}

	@Test
	fun `skal maskere felt med custom maskertVerdi`() {
		val person = PersonMedCustomMaskering(navn = "Kari Nordmann", epost = "kari@example.com")

		val maskertPerson = maskerObjekt(person)

		assertEquals("SKJULT", maskertPerson.navn)
		assertEquals("kari@example.com", maskertPerson.epost)
	}

	@Test
	fun `skal ikke endre objekt uten Maskert-annotasjoner`() {
		val person = PersonUtenMaskering(navn = "Per Hansen", adresse = "Storgata 1")

		val maskertPerson = maskerObjekt(person)

		assertEquals("Per Hansen", maskertPerson.navn)
		assertEquals("Storgata 1", maskertPerson.adresse)
	}

	@Test
	fun `skal maskere felt i nestede objekter`() {
		val person =
			NestedPerson(
				navn = "Line Berg",
				kontaktinfo = Kontaktinfo(telefon = "12345678", epost = "line@example.com"),
			)

		val maskertPerson = maskerObjekt(person)

		assertEquals("*******", maskertPerson.navn)
		assertEquals("*******", maskertPerson.kontaktinfo.telefon)
		assertEquals("line@example.com", maskertPerson.kontaktinfo.epost)
	}

	@Test
	fun `skal maskere felt i lister med objekter`() {
		val person =
			PersonMedListe(
				navn = "Erik Johansen",
				adresser =
					listOf(
						Adresse(gate = "Bakkeveien 2", postnummer = "0123"),
						Adresse(gate = "Fjellgata 5", postnummer = "0456"),
					),
			)

		val maskertPerson = maskerObjekt(person)

		assertEquals("*******", maskertPerson.navn)
		assertEquals(2, maskertPerson.adresser.size)
		assertEquals("*******", maskertPerson.adresser[0].gate)
		assertEquals("0123", maskertPerson.adresser[0].postnummer)
		assertEquals("*******", maskertPerson.adresser[1].gate)
		assertEquals("0456", maskertPerson.adresser[1].postnummer)
	}

	@Test
	fun `skal håndtere tom liste`() {
		val person = PersonMedListe(navn = "Test Person", adresser = emptyList())

		val maskertPerson = maskerObjekt(person)

		assertEquals("*******", maskertPerson.navn)
		assertEquals(0, maskertPerson.adresser.size)
	}

	@Test
	fun `skal håndtere enum-verdier`() {
		val person = PersonMedEnum(navn = "Anna Larsen", status = Status.AKTIV)

		val maskertPerson = maskerObjekt(person)

		assertEquals("*******", maskertPerson.navn)
		assertEquals(Status.AKTIV, maskertPerson.status)
	}

	@Test
	fun `skal returnere primitiver uendret`() {
		val tekst = "Hello World"
		val tall = 42
		val boolsk = true

		assertEquals(tekst, maskerObjekt(tekst))
		assertEquals(tall, maskerObjekt(tall))
		assertEquals(boolsk, maskerObjekt(boolsk))
	}

	@Test
	fun `skal håndtere map med objekter`() {
		val map =
			mapOf(
				"person1" to EnkelPerson(navn = "Test1", alder = 25),
				"person2" to EnkelPerson(navn = "Test2", alder = 30),
			)

		val maskertMap = maskerObjekt(map)

		assertEquals("*******", maskertMap["person1"]?.navn)
		assertEquals(25, maskertMap["person1"]?.alder)
		assertEquals("*******", maskertMap["person2"]?.navn)
		assertEquals(30, maskertMap["person2"]?.alder)
	}

	@Test
	fun `skal returnere nytt objekt, ikke modifisere original`() {
		val original = EnkelPerson(navn = "Original Navn", alder = 40)

		val maskert = maskerObjekt(original)

		assertNotEquals(original.navn, maskert.navn)
		assertEquals("Original Navn", original.navn)
		assertEquals("*******", maskert.navn)
	}

	@Test
	fun `skal håndtere liste med primitiver`() {
		val liste = listOf("test1", "test2", "test3")

		val maskertListe = maskerObjekt(liste)

		assertEquals(3, maskertListe.size)
		assertEquals("test1", maskertListe[0])
		assertEquals("test2", maskertListe[1])
		assertEquals("test3", maskertListe[2])
	}

	@Test
	fun `skal håndtere numeriske typer`() {
		assertEquals(42, maskerObjekt(42))
		assertEquals(42L, maskerObjekt(42L))
		assertEquals(3.14, maskerObjekt(3.14))
		assertEquals(3.14f, maskerObjekt(3.14f))
	}
}
