package no.nav.persondataapi.service

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.inntekt.generated.model.Loennsinntekt
import no.nav.persondataapi.integrasjon.ereg.client.EregClient
import no.nav.persondataapi.integrasjon.inntekt.client.InntektClient
import no.nav.persondataapi.integrasjon.inntekt.client.KontrollPeriode
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.konfigurasjon.teamLogsMarker
import no.nav.persondataapi.responstracing.erTraceLoggingAktvert
import no.nav.persondataapi.rest.domene.InntektInformasjon
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import no.nav.persondataapi.tracelogging.traceLogg
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class InntektService(
    private val inntektClient: InntektClient,
    private val eregClient: EregClient,
    private val brukertilgangService: BrukertilgangService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentInntekterForPerson(personIdent: PersonIdent, utvidet: Boolean = false): InntektResultat {
        val kontrollperiode = KontrollPeriode(
            LocalDate.now().minusYears(if (utvidet) 10 else 5),
            LocalDate.now()
        )
        // Hent inntekter fra InntektClient
        val inntektResponse = inntektClient.hentInntekter(personIdent = personIdent, periode = kontrollperiode)
        logger.info("Hentet inntekter for $personIdent (utvidet = $utvidet), status ${inntektResponse.statusCode}")
        if (erTraceLoggingAktvert()){
            traceLogg(
                logger = logger,
                kilde = "Inntekt",
                personIdent=personIdent,
                unit = inntektResponse
            )
        }
        // Håndter feil fra InntektClient
        when (inntektResponse.statusCode) {
            404 -> return InntektResultat.PersonIkkeFunnet
            403 -> return InntektResultat.IngenTilgang
            500 -> return InntektResultat.FeilIBaksystem
            !in 200..299 -> return InntektResultat.FeilIBaksystem
        }

        // Prosesser lønnsinntekt
        val lønnsinntekt = inntektResponse.data?.data
            .orEmpty()
            .flatMap { historikk ->
                val arbeidsgiver = if (historikk.opplysningspliktig.matches("\\d{9}".toRegex())) {
                    eregClient.hentOrganisasjon(historikk.opplysningspliktig)
                } else null

                var respons = historikk.versjoner.nyeste()
                    ?.inntektListe
                    ?.filterIsInstance<Loennsinntekt>()
                    ?.map { loenn ->
                        InntektInformasjon.Lønnsdetaljer(
                            arbeidsgiver = arbeidsgiver?.navn?.sammensattnavn,
                            periode = historikk.maaned,
                            arbeidsforhold = "",
                            stillingsprosent = "",
                            lønnstype = loenn.beskrivelse,
                            antall = loenn.antall,
                            beløp = loenn.beloep,
                            harFlereVersjoner = historikk.harHistorikkPåNormallønn()
                        )
                    }
                    .orEmpty()
                if (respons.isEmpty() && historikk.versjoner.eldste()?.inntektListe
                        ?.filterIsInstance<Loennsinntekt>()?.isEmpty() == false) {

                    respons = listOf(InntektInformasjon.Lønnsdetaljer(
                        arbeidsgiver = arbeidsgiver?.navn?.sammensattnavn,
                        periode = historikk.maaned,
                        arbeidsforhold = "",
                        stillingsprosent = "",
                        lønnstype = historikk.versjoner.eldste()?.inntektListe?.first()?.type,
                        antall = null,
                        beløp = BigDecimal.ZERO,
                        harFlereVersjoner = true
                    ))
                }
                respons
            }

        logger.info("Fant ${lønnsinntekt.size} lønnsinntekt(er) for $personIdent")

        var respons = InntektInformasjon(lønnsinntekt = lønnsinntekt)

        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente inntekter for $personIdent. Maskerer responsen")
           respons = maskerObjekt(respons)
        }

        return InntektResultat.Success(respons)
    }
}

sealed class InntektResultat {
    data class Success(val data: InntektInformasjon) : InntektResultat()
    data object IngenTilgang : InntektResultat()
    data object PersonIkkeFunnet : InntektResultat()
    data object FeilIBaksystem : InntektResultat()
}
