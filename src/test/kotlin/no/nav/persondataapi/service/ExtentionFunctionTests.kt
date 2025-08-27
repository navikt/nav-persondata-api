package no.nav.persondataapi.service

import no.nav.inntekt.generated.model.HistorikkData
import no.nav.inntekt.generated.model.Inntekt
import no.nav.inntekt.generated.model.Inntektsinformasjon
import no.nav.inntekt.generated.model.Loennsinntekt
import no.nav.inntekt.generated.model.YtelseFraOffentlige
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime

class ExtentionFunctionTests {

    @Test
    fun historikkDataSkalHåndtereNullVersjoner() {

        val historikkData = HistorikkData(
            maaned = "2020-01-01",
            opplysningspliktig = "123",
            underenhet = "1234",
            norskident = "12345678901",
            versjoner = emptyList(),
        )
        Assertions.assertFalse(historikkData.historikkPaaNormalLoenn())
    }
    @Test
    fun kunYtelseFraOffentligeSkalIkkeTelleIHistorikken() {

        val historikkData = HistorikkData(
            maaned = "2020-01-01",
            opplysningspliktig = "123",
            underenhet = "1234",
            norskident = "12345678901",
            versjoner = listOf(
                Inntektsinformasjon(
                    maaned = "1234",
                    opplysningspliktig = "1234",
                    underenhet = "1234",
                    norskident = "12345678901",
                    oppsummeringstidspunkt = OffsetDateTime.now().minusDays(30),
                    inntektListe = listOf(
                        YtelseFraOffentlige (
                            beloep = BigDecimal.valueOf(12344),
                            fordel = "kontantytelse",
                            beskrivelse ="",
                            inngaarIGrunnlagForTrekk = false,
                            utloeserArbeidsgiveravgift = false,
                            type = "YtelseFraOffentlige"


                        )
                    )
                ),
                Inntektsinformasjon(
                    maaned = "1234",
                    opplysningspliktig = "1234",
                    underenhet = "1234",
                    norskident = "12345678901",
                    oppsummeringstidspunkt = OffsetDateTime.now().minusDays(10),
                    inntektListe = listOf(
                        YtelseFraOffentlige (
                            beloep = BigDecimal.valueOf(12344),
                            fordel = "kontantytelse",
                            beskrivelse ="",
                            inngaarIGrunnlagForTrekk = false,
                            utloeserArbeidsgiveravgift = false,
                            type = "YtelseFraOffentlige"
                        )
                    )
                )

            ),
        )
        Assertions.assertFalse(historikkData.historikkPaaNormalLoenn())
    }

    @Test
    fun kunEttInnslagAvLonnsInntektSkalIkkeTelleIHistorikken() {

        val historikkData = HistorikkData(
            maaned = "2020-01-01",
            opplysningspliktig = "123",
            underenhet = "1234",
            norskident = "12345678901",
            versjoner = listOf(
                Inntektsinformasjon(
                    maaned = "1234",
                    opplysningspliktig = "1234",
                    underenhet = "1234",
                    norskident = "12345678901",
                    oppsummeringstidspunkt = OffsetDateTime.now().minusDays(30),
                    inntektListe = listOf(
                        YtelseFraOffentlige (
                            beloep = BigDecimal.valueOf(12344),
                            fordel = "kontantytelse",
                            beskrivelse ="",
                            inngaarIGrunnlagForTrekk = false,
                            utloeserArbeidsgiveravgift = false,
                            type = "YtelseFraOffentlige"


                        )
                    )
                ),
                Inntektsinformasjon(
                    maaned = "1234",
                    opplysningspliktig = "1234",
                    underenhet = "1234",
                    norskident = "12345678901",
                    oppsummeringstidspunkt = OffsetDateTime.now().minusDays(10),
                    inntektListe = listOf(
                        Loennsinntekt (
                            beloep = BigDecimal.valueOf(12344),
                            fordel = "kontantytelse",
                            beskrivelse ="",
                            inngaarIGrunnlagForTrekk = false,
                            utloeserArbeidsgiveravgift = false,
                            type = "YtelseFraOffentlige"
                        )
                    )
                )

            ),
        )
        Assertions.assertFalse(historikkData.historikkPaaNormalLoenn())
    }

    @Test
    fun MerEnEttInnslagAvLonnsInntektSkalIkkeTelleIHistorikken() {

        val historikkData = HistorikkData(
            maaned = "2020-01-01",
            opplysningspliktig = "123",
            underenhet = "1234",
            norskident = "12345678901",
            versjoner = listOf(
                Inntektsinformasjon(
                    maaned = "1234",
                    opplysningspliktig = "1234",
                    underenhet = "1234",
                    norskident = "12345678901",
                    oppsummeringstidspunkt = OffsetDateTime.now().minusDays(30),
                    inntektListe = listOf(
                        Loennsinntekt (
                            beloep = BigDecimal.valueOf(12344),
                            fordel = "kontantytelse",
                            beskrivelse ="",
                            inngaarIGrunnlagForTrekk = false,
                            utloeserArbeidsgiveravgift = false,
                            type = "YtelseFraOffentlige"


                        )
                    )
                ),
                Inntektsinformasjon(
                    maaned = "1234",
                    opplysningspliktig = "1234",
                    underenhet = "1234",
                    norskident = "12345678901",
                    oppsummeringstidspunkt = OffsetDateTime.now().minusDays(10),
                    inntektListe = listOf(
                        Loennsinntekt (
                            beloep = BigDecimal.valueOf(12344),
                            fordel = "kontantytelse",
                            beskrivelse ="",
                            inngaarIGrunnlagForTrekk = false,
                            utloeserArbeidsgiveravgift = false,
                            type = "YtelseFraOffentlige"
                        )
                    )
                )

            ),
        )
        Assertions.assertTrue(historikkData.historikkPaaNormalLoenn())
    }

}