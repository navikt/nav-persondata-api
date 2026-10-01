package no.nav.persondataapi.rest.oppslag

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.domene.PersonInformasjon
import no.nav.persondataapi.rest.domene.PersonInformasjonV1Dto
import no.nav.persondataapi.service.PersonopplysningerResultat
import no.nav.persondataapi.service.PersonopplysningerService
import no.nav.persondataapi.unleash.FeatureToggleService
import no.nav.persondataapi.unleash.Toggle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class PersonopplysningerControllerTest {
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val controller = PersonopplysningerController(personopplysningerService, featureToggleService)

    private val etFamiliemedlem =
        PersonInformasjon.Familiemedlem(
            ident = "11111111111",
            rolle = "BARN",
            fornavn = "Liten",
            etternavn = "Nordmann",
        )

    private val personopplysninger =
        PersonInformasjon(
            aktørId = "12345678901",
            familemedlemmer = listOf(etFamiliemedlem),
            statsborgerskap = listOf("Norge"),
            navn = PersonInformasjon.Navn(fornavn = "Ola", mellomnavn = null, etternavn = "Nordmann"),
            adresse = null,
            epost = "syntetisk@example.com",
            sivilstand = null,
            alder = 40,
            adressebeskyttelse = PersonInformasjon.Skjerming.UGRADERT,
            fødselsdato = "1984-01-01",
            dødsdato = null,
            navKontor = null,
        )

    @Test
    fun `returnerer ny List-struktur for familemedlemmer når feature-flagg er PÅ`() =
        runBlocking {
            every { featureToggleService.isEnabled(Toggle.WATSON_SOK_V_1_2) } returns true
            coEvery { personopplysningerService.hentPersonopplysningerForPerson(any(), any()) } returns
                PersonopplysningerResultat.Success(personopplysninger)

            val respons = controller.hentPersonopplysninger(OppslagRequestDto(PersonIdent("12345678901")), null)

            assertEquals(HttpStatus.OK, respons.statusCode)
            val data = respons.body?.data
            assertInstanceOf(PersonInformasjon::class.java, data)
            val personInfo = data as PersonInformasjon
            assertEquals(1, personInfo.familemedlemmer.size)
            assertEquals("11111111111", personInfo.familemedlemmer[0].ident)
            assertEquals("BARN", personInfo.familemedlemmer[0].rolle)
            assertEquals("Liten", personInfo.familemedlemmer[0].fornavn)
            assertEquals("syntetisk@example.com", personInfo.epost)
        }

    @Test
    fun `returnerer gammel Map-struktur for familemedlemmer når feature-flagg er AV`() =
        runBlocking {
            every { featureToggleService.isEnabled(Toggle.WATSON_SOK_V_1_2) } returns false
            coEvery { personopplysningerService.hentPersonopplysningerForPerson(any(), any()) } returns
                PersonopplysningerResultat.Success(personopplysninger)

            val respons = controller.hentPersonopplysninger(OppslagRequestDto(PersonIdent("12345678901")), null)

            assertEquals(HttpStatus.OK, respons.statusCode)
            val data = respons.body?.data
            assertInstanceOf(PersonInformasjonV1Dto::class.java, data)
            val v1Dto = data as PersonInformasjonV1Dto
            assertEquals(mapOf("11111111111" to "BARN"), v1Dto.familemedlemmer)
        }

    @Test
    fun `OpenAPI beskriver e-post i nytt responsformat og viser 200-eksempel`() {
        val metode =
            PersonopplysningerController::class.java.getMethod(
                "hentPersonopplysninger",
                OppslagRequestDto::class.java,
                String::class.java,
            )
        val respons = metode.getAnnotation(ApiResponses::class.java).value.single { it.responseCode == "200" }
        val innhold = respons.content.single()

        assertTrue(innhold.schema.oneOf.contains(PersonopplysningerResponsV2Dokumentasjon::class))
        assertTrue(innhold.schema.oneOf.contains(PersonopplysningerResponsV1Dokumentasjon::class))
        val nyttEksempel = JsonUtils.mapper.readTree(innhold.examples.first { it.name.contains("KRR") }.value)
        assertEquals("syntetisk@example.com", nyttEksempel["data"]["epost"].stringValue())
        val gammeltEksempel = JsonUtils.mapper.readTree(innhold.examples.first { it.name.contains("Gammelt") }.value)
        assertEquals(false, gammeltEksempel["data"].has("epost"))
        val skjemaer = ModelConverters.getInstance().readAll(PersonopplysningerResponsV2Dokumentasjon::class.java)
        val personSkjema = skjemaer.values.firstOrNull { it.properties?.containsKey("epost") == true }
        assertNotNull(personSkjema)
        assertEquals(true, personSkjema?.properties?.get("epost")?.nullable)
    }

    @Test
    fun `returnerer 403 ved IngenTilgang uavhengig av feature-flagg`() =
        runBlocking {
            every { featureToggleService.isEnabled(any()) } returns false
            coEvery { personopplysningerService.hentPersonopplysningerForPerson(any(), any()) } returns
                PersonopplysningerResultat.IngenTilgang

            val respons = controller.hentPersonopplysninger(OppslagRequestDto(PersonIdent("12345678901")), null)

            assertEquals(HttpStatus.FORBIDDEN, respons.statusCode)
        }

    @Test
    fun `returnerer 404 ved PersonIkkeFunnet uavhengig av feature-flagg`() =
        runBlocking {
            every { featureToggleService.isEnabled(any()) } returns false
            coEvery { personopplysningerService.hentPersonopplysningerForPerson(any(), any()) } returns
                PersonopplysningerResultat.PersonIkkeFunnet

            val respons = controller.hentPersonopplysninger(OppslagRequestDto(PersonIdent("12345678901")), null)

            assertEquals(HttpStatus.NOT_FOUND, respons.statusCode)
        }
}
