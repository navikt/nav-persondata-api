package no.nav.persondataapi.service


import no.nav.persondataapi.configuration.JsonUtils
import no.nav.persondataapi.domain.AaregResultat
import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.ereg.client.EregRespons
import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.rest.domain.PersonInformasjon

import no.nav.persondataapi.service.dataproviders.GrunnlagsType
import no.nav.persondataapi.service.dataproviders.GrunnlagsdelResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets
import kotlin.test.assertNotNull


class RestDomainMapperTest {

    @Test
    fun test(){




        val eregjsonString = readJsonFromFile("testrespons/EregResponsSample.json")
        val eregRespons: EregRespons = JsonUtils.fromJson(eregjsonString)

        val eregjsonString2 = readJsonFromFile("testrespons/EregResponsSample2.json")
        val eregRespons2: EregRespons = JsonUtils.fromJson(eregjsonString2)


        val jsonString = readJsonFromFile("testrespons/aaregGrunnlagsdataResponsSample.json")
        val aaregRespons: AaregResultat = JsonUtils.fromJson(jsonString)
        val grunnlag = GrunnlagsData(
            ident = "1234",
            saksbehandlerId = "1234",
            utbetalingRespons = null,
            personDataRespons = null,
            inntektDataRespons = null,
                aAaregDataRespons = GrunnlagsdelResultat(
                    type = GrunnlagsType.ARBEIDSFORHOLD,
                    data=aaregRespons,
                    status = 200
                ),
            eregDataRespons = mapOf(
                Pair("986929150",eregRespons),
                Pair("984886519",eregRespons2),

            )
        )
        val frontEndGrunnlagsData = grunnlag.getArbeidsGiverInformasjon()
        Assertions.assertTrue(frontEndGrunnlagsData.lopendeArbeidsforhold.isNotEmpty())
        Assertions.assertTrue(frontEndGrunnlagsData.historikk.isNotEmpty())
        val arbeidsforhold = frontEndGrunnlagsData.lopendeArbeidsforhold.first()
        Assertions.assertNotNull(arbeidsforhold)
        Assertions.assertNotNull(arbeidsforhold.adresse)
        Assertions.assertNotNull(arbeidsforhold.arbeidsgiver)
        Assertions.assertTrue (arbeidsforhold.ansettelsesDetaljer.isNotEmpty())
        Assertions.assertEquals("TEST GATA 75, 5252 SØREIDGREND",arbeidsforhold.adresse)
        Assertions.assertEquals("SAUEFABRIKK",arbeidsforhold.arbeidsgiver)

    }

    @Test
    fun kanOversettePdlTilPersonInformasjon(){




        val pdlString = readJsonFromFile("testrespons/PdlResponsSample.json")
        val pdlRespons: Person = JsonUtils.fromJson(pdlString)


        val grunnlag = GrunnlagsData(
            ident = "1234",
            saksbehandlerId = "1234",
            utbetalingRespons = null,
            personDataRespons = GrunnlagsdelResultat(
                type = GrunnlagsType.PERSONDATA,
                data=pdlRespons,
                status = 200),
            inntektDataRespons = null,
            aAaregDataRespons = null
            )

        val personInformasjon =  grunnlag.getPersonInformasjon()
        Assertions.assertNotNull(personInformasjon)
        Assertions.assertEquals("HANS JACOB ASLAKSRUD MELBY",personInformasjon.navn)
        Assertions.assertEquals("Slalåmveien 62, 1350",personInformasjon.adresse)

    }



}

private fun readJsonFromFile(filename: String): String {
    val resource = ClassPathResource(filename)
    val inputStream = resource.inputStream
    return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
}