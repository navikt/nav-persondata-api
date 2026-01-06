package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.utbetaling.client.UtbetalingClient
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.konfigurasjon.teamLogsMarker
import no.nav.persondataapi.responstracing.erTraceLoggingAktvert
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.domene.Ytelse
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class YtelseService(
    private val utbetalingClient: UtbetalingClient,
    private val brukertilgangService: BrukertilgangService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentYtelserForPerson(personIdent: PersonIdent, utvidet: Boolean): YtelserResultat {
        val utbetalingResponse = utbetalingClient.hentUtbetalingerForBruker(personIdent, utvidet)
        logger.info("Hentet ${if (utvidet) "utvidete " else ""}ytelser for $personIdent, status ${utbetalingResponse.statusCode}")
        if (erTraceLoggingAktvert()){
            logger.info(teamLogsMarker,"Logging aktivert - full ytelser-respons for {}: {}", personIdent, JsonUtils.toJson(utbetalingResponse).toPrettyString())
        }
        when (utbetalingResponse.statusCode) {
            404 -> return YtelserResultat.PersonIkkeFunnet
            403, 401 -> return YtelserResultat.IngenTilgang
            500 -> return YtelserResultat.FeilIBaksystem
            !in 200..299 -> return YtelserResultat.FeilIBaksystem
        }

        if (utbetalingResponse.data?.utbetalinger.isNullOrEmpty()) {
            logger.info("Fant ingen ytelser for $personIdent")
            return YtelserResultat.Success(emptyList())
        }

        val utbetalinger = utbetalingResponse.data.utbetalinger
        var ytelser: List<Ytelse> = utbetalinger
            .asSequence()
            .flatMap { it.ytelseListe.asSequence() }
            .filter { it.ytelsestype != null }
            .filter { it.ytelsestype != "Feriepenger" }
            .groupBy { it.ytelsestype }
            .map { (type, liste) ->
                val perioder = liste.map { ytelse ->
                    Ytelse.PeriodeInformasjon(
                        periode = Ytelse.Periode(
                            fom = ytelse.ytelsesperiode.fom,
                            tom = ytelse.ytelsesperiode.tom
                        ),
                        beløp = ytelse.ytelseNettobeloep,
                        bruttoBeløp = ytelse.ytelseskomponentersum,
                        refundertForOrg = ytelse.refundertForOrg?.ident ?: "UKJENT",
                        kilde = "SOKOS",
                        info = ytelse.bilagsnummer
                    )
                }
                Ytelse(stonadType = type!!, perioder)
            }
            .toList()

        logger.info("Fant ${ytelser.size} ytelse(r) for $personIdent")

        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente ytelser for $personIdent. Maskerer responsen")
            ytelser = maskerObjekt(ytelser)
        }

        return YtelserResultat.Success(ytelser)
    }
}

sealed class YtelserResultat {
    data class Success(val data: List<Ytelse>) : YtelserResultat()
    data object IngenTilgang : YtelserResultat()
    data object PersonIkkeFunnet : YtelserResultat()
    data object FeilIBaksystem : YtelserResultat()
}
