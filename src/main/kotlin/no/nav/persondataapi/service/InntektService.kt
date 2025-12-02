package no.nav.persondataapi.service

import no.nav.inntekt.generated.model.Loennsinntekt
import no.nav.persondataapi.integrasjon.ereg.client.EregClient
import no.nav.persondataapi.integrasjon.inntekt.client.InntektClient
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.konfigurasjon.teamLogsMarker
import no.nav.persondataapi.responstracing.erTraceLoggingAktvert
import no.nav.persondataapi.rest.domene.InntektInformasjon
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class InntektService(
    private val inntektClient: InntektClient,
    private val eregClient: EregClient,
    private val brukertilgangService: BrukertilgangService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentInntekterForPerson(personIdent: PersonIdent): InntektResultat {
        // Hent inntekter fra InntektClient
        val inntektResponse = inntektClient.hentInntekter(personIdent)
        logger.info("Hentet inntekter for $personIdent, status ${inntektResponse.statusCode}")
        if (erTraceLoggingAktvert()){
            logger.info(teamLogsMarker,"Logging aktivert - full Inntekt-respons for {}: {}", personIdent, JsonUtils.toJson(inntektResponse).toPrettyString())
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
                val arbeidsgiver = eregClient.hentOrganisasjon(historikk.opplysningspliktig)

                historikk.versjoner.nyeste()
                    ?.inntektListe
                    ?.filterIsInstance<Loennsinntekt>()
                    ?.map { loenn ->
                        InntektInformasjon.Lønnsdetaljer(
                            arbeidsgiver = arbeidsgiver.navn?.sammensattnavn,
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
