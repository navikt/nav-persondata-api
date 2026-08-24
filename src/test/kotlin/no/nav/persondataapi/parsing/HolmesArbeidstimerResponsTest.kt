package no.nav.persondataapi.parsing

import no.nav.persondataapi.integrasjon.aap.meldekort.client.alleTimerArbeidSegmenter
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.HolmesArbeidstimerRespons
import no.nav.persondataapi.konfigurasjon.JsonUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import kotlin.test.Test

/**
 * Parser en reell (anonymisert) respons fra `/holmes/arbeidstimer`, mottatt
 * fra team AAP under utviklingen av SEARCH-30. Personen har et reelt
 * innsendt meldekort med rapporterte timer for perioden 3.–16. august 2026,
 * som gir god dekning for både «flere fine-granulerte segmenter i én
 * meldeperiode» og «hull i meldeperiode-rekken» (ingen meldeperiode for
 * 6.–19. juli).
 */
class HolmesArbeidstimerResponsTest {
    @Test
    fun kanLeseReellRespons() {
        val jsonString = lesJsonFraFil("testrespons/HolmesArbeidstimerReellSverdi.json")
        val respons: HolmesArbeidstimerRespons = JsonUtils.fromJson(jsonString)

        assertEquals("13429149309", respons.personIdent)
        assertEquals(12, respons.meldeperioder.size)
    }

    @Test
    fun `meldeperiode med faktisk rapportert arbeid har flere fine-granulerte timerArbeid-segmenter`() {
        val jsonString = lesJsonFraFil("testrespons/HolmesArbeidstimerReellSverdi.json")
        val respons: HolmesArbeidstimerRespons = JsonUtils.fromJson(jsonString)

        val augustPeriode =
            respons.meldeperioder.single { it.periodeFom == LocalDate.parse("2026-08-03") }

        assertEquals(6, augustPeriode.timerArbeid.size)
        assertEquals(
            BigDecimal("8"),
            augustPeriode.timerArbeid.sumOf { it.timerArbeidet },
        )
    }

    @Test
    fun `alleTimerArbeidSegmenter flater ut segmenter fra alle meldeperioder, inkludert hull i rekken`() {
        val jsonString = lesJsonFraFil("testrespons/HolmesArbeidstimerReellSverdi.json")
        val respons: HolmesArbeidstimerRespons = JsonUtils.fromJson(jsonString)

        val alleSegmenter = respons.alleTimerArbeidSegmenter()

        // 11 meldeperioder med 1 segment + 1 meldeperiode (august) med 6 segmenter
        assertEquals(17, alleSegmenter.size)
        // Hull i meldeperiode-rekken (ingen data for 6.–19. juli) skal ikke
        // krasje parsingen — det finnes rett og slett ingen segmenter i det
        // intervallet.
        val segmenterIHullet = alleSegmenter.filter { it.periodeFom.isAfter(LocalDate.parse("2026-07-05")) }
        assertEquals(true, segmenterIHullet.all { it.periodeFom.isAfter(LocalDate.parse("2026-07-19")) })
    }
}

private fun lesJsonFraFil(filename: String): String {
    val resource = ClassPathResource(filename)
    val inputStream = resource.inputStream
    return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
}
