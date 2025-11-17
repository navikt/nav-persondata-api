package no.nav.persondataapi.parsing

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.integrasjon.aareg.client.AaregClient
import no.nav.persondataapi.integrasjon.aareg.client.AaregDataResultat
import no.nav.persondataapi.integrasjon.ereg.client.EregClient
import no.nav.persondataapi.integrasjon.ereg.client.EregRespons
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.ArbeidsforholdResultat
import no.nav.persondataapi.service.ArbeidsforholdService
import no.nav.persondataapi.service.BrukertilgangService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime

class AaregParseResponsTest {
	@Test
	fun kanLeseAaregResponsFraProduksjon() =
		runBlocking {
			val jsonString = readJsonFromFile("testrespons/aaregGrunnlagsdataResponsSample.json")
			val aaregRespons: AaregDataResultat = JsonUtils.fromJson(jsonString)

			val brukertilgangService = mockk<BrukertilgangService>()
			val aaregClient = mockk<AaregClient>()
			val eregClient = mockk<EregClient>()

			every { brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(any()) } returns true
			every { eregClient.hentOrganisasjon("984886519") } returns lagEregRespons("984886519", "TELNOR AS")
			every { eregClient.hentOrganisasjon("986929150") } returns lagEregRespons("999888778", "COMPUTAS")
			every { eregClient.hentOrganisasjon("976967631") } returns lagEregRespons("999888778", "COMPUTAS")
			every { eregClient.hentOrganisasjon("986352325") } returns lagEregRespons("999888778", "COMPUTAS")
			every { aaregClient.hentArbeidsforhold(any()) } returns aaregRespons

			val service = ArbeidsforholdService(aaregClient, eregClient, brukertilgangService)
			val resultat = service.hentArbeidsforholdForPerson(PersonIdent("21047541120"))
			assertTrue(resultat is ArbeidsforholdResultat.Success)
			val data = (resultat as ArbeidsforholdResultat.Success).data
			assertEquals(1, data.løpendeArbeidsforhold.size)
			assertEquals(3, data.historikk.size)

			// sjekk at kun en ansattDetalj under løpende har null til og med dato
			val alle_ansattdetaljer = data.løpendeArbeidsforhold.flatMap { it.ansettelsesDetaljer }.filter { it.periode.tom == null }
			assertEquals(1, alle_ansattdetaljer.size)
			// sjekk at ingenansattDetalj under historisk har null til og med dato
			val alle_ansattdetaljer_historiske = data.historikk.flatMap { it.ansettelsesDetaljer }.filter { it.periode.tom == null }
			assertTrue(alle_ansattdetaljer_historiske.isEmpty())
		}
}

private fun readJsonFromFile(filename: String): String {
	val resource = ClassPathResource(filename)
	val inputStream = resource.inputStream
	return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
}

private fun lagEregRespons(
	orgnummer: String,
	navn: String?,
): EregRespons =
	EregRespons(
		organisasjonsnummer = orgnummer,
		type = "AS",
		navn =
			if (navn != null) {
				no.nav.persondataapi.integrasjon.ereg.client.Navn(
					sammensattnavn = navn,
					navnelinje1 = navn,
					bruksperiode =
						no.nav.persondataapi.integrasjon.ereg.client
							.PeriodeTid(fom = LocalDateTime.now()),
					gyldighetsperiode =
						no.nav.persondataapi.integrasjon.ereg.client
							.PeriodeDato(fom = LocalDate.now()),
				)
			} else {
				null
			},
		organisasjonDetaljer = null,
		virksomhetDetaljer = null,
	)
