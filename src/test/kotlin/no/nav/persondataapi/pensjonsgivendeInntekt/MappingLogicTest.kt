package no.nav.persondataapi.pensjonsgivendeInntekt

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MappingLogicTest {

    @Test
    fun MappingSlalSlåSammenAlleNæringsInnekter(){
        val sigrunModell = SigrunPensjonsgivendeInntektResponse(
            inntektsaar = "2020",
            pensjonsgivendeInntekt = listOf(PensjonsgivendeInntekt(
                skatteordning = "FAST",
                datoForFastsetting = "2021-01-01",
                pensjonsgivendeInntektAvLoennsinntekt = 900000,
                pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = 100000,
                pensjonsgivendeInntektAvNaeringsinntekt = 100,
                pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = 100
            ))
        )
        val nymodell = listOf<SigrunPensjonsgivendeInntektResponse>(sigrunModell).toPensjonsgivendeInntektOppummering()
        Assertions.assertEquals(1, nymodell.size,"Feil antall objekter i ny modell")
        Assertions.assertEquals("2020",nymodell.first().inntektsaar,"Inntekt år er mappet feil")
        Assertions.assertEquals(1000000, nymodell.first().lønnsinntekt,"Lønns inntekt er ikke summert korrekt")
        Assertions.assertEquals(200, nymodell.first().næringsinntekt,"Næringsinntekt er ikke summert korrekt")
    }

    @Test
    fun mappingSkalSlåSammenAlleNæringsinntekterPåTversAvSkatteordning(){
        val sigrunModell = SigrunPensjonsgivendeInntektResponse(
            inntektsaar = "2020",
            pensjonsgivendeInntekt = listOf(
                PensjonsgivendeInntekt(
                    skatteordning = "FAST",
                    datoForFastsetting = "2021-01-01",
                    pensjonsgivendeInntektAvLoennsinntekt = 900000,
                    pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = 100000,
                    pensjonsgivendeInntektAvNaeringsinntekt = 100,
                    pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = 100
                    ),
                PensjonsgivendeInntekt(
                    skatteordning = "SVALBARD",
                    datoForFastsetting = "2021-01-01",
                    pensjonsgivendeInntektAvLoennsinntekt = 900000,
                    pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = 100000,
                    pensjonsgivendeInntektAvNaeringsinntekt = 100,
                    pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = 100
                )
            )
        )
        val nymodell = listOf<SigrunPensjonsgivendeInntektResponse>(sigrunModell).toPensjonsgivendeInntektOppummering()
        Assertions.assertEquals(1, nymodell.size,"Feil antall objekter i ny modell")
        Assertions.assertEquals("2020",nymodell.first().inntektsaar,"Inntekt år er mappet feil")
        Assertions.assertEquals(2000000, nymodell.first().lønnsinntekt,"Lønns inntekt er ikke summert korrekt")
        Assertions.assertEquals(400, nymodell.first().næringsinntekt,"Næringsinntekt er ikke summert korrekt")
    }
    @Test
    fun MappingSkalSlåSammenAlleNæringsInnekterPåTversAvSkatteOrdningMenIkkePåTversAvÅr(){
        val sigrunModell2020 = SigrunPensjonsgivendeInntektResponse(
            inntektsaar = "2020",
            pensjonsgivendeInntekt = listOf(
                PensjonsgivendeInntekt(
                    skatteordning = "FAST",
                    datoForFastsetting = "2021-01-01",
                    pensjonsgivendeInntektAvLoennsinntekt = 900000,
                    pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = 100000,
                    pensjonsgivendeInntektAvNaeringsinntekt = 100,
                    pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = 100
                ),
                PensjonsgivendeInntekt(
                    skatteordning = "SVALBARD",
                    datoForFastsetting = "2021-01-01",
                    pensjonsgivendeInntektAvLoennsinntekt = 900000,
                    pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = 100000,
                    pensjonsgivendeInntektAvNaeringsinntekt = 100,
                    pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = 100
                )
            )
        )
        val sigrunModell2021 = SigrunPensjonsgivendeInntektResponse(
            inntektsaar = "2021",
            pensjonsgivendeInntekt = listOf(
                PensjonsgivendeInntekt(
                    skatteordning = "FAST",
                    datoForFastsetting = "2021-01-01",
                    pensjonsgivendeInntektAvLoennsinntekt = 900000,
                    pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = 100000,
                    pensjonsgivendeInntektAvNaeringsinntekt = 100,
                    pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = 100
                ),
                PensjonsgivendeInntekt(
                    skatteordning = "SVALBARD",
                    datoForFastsetting = "2021-01-01",
                    pensjonsgivendeInntektAvLoennsinntekt = 900000,
                    pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = 100000,
                    pensjonsgivendeInntektAvNaeringsinntekt = 100,
                    pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = 100
                )
            )
        )
        val nymodell = listOf<SigrunPensjonsgivendeInntektResponse>(sigrunModell2020,sigrunModell2021).toPensjonsgivendeInntektOppummering()
        Assertions.assertEquals(2, nymodell.size,"Feil antall objekter i ny modell")
        Assertions.assertEquals("2020",nymodell.first().inntektsaar,"Inntekt år er mappet feil")
        Assertions.assertEquals(2000000, nymodell.first().lønnsinntekt,"Lønns inntekt er ikke summert korrekt")
        Assertions.assertEquals(400, nymodell.first().næringsinntekt,"Næringsinntekt er ikke summert korrekt")

        Assertions.assertEquals("2021",nymodell.last().inntektsaar,"Inntekt år er mappet feil")
        Assertions.assertEquals(2000000, nymodell.last().lønnsinntekt,"Lønns inntekt er ikke summert korrekt")
        Assertions.assertEquals(400, nymodell.last().næringsinntekt,"Næringsinntekt er ikke summert korrekt")
    }

}