package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.utbetaling.client.UtbetalingClient
import no.nav.persondataapi.rest.domene.Stønad
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StønadService(
    private val utbetalingClient: UtbetalingClient,
    private val brukertilgangService: BrukertilgangService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentStønaderForPerson(personIdent: String): StønadResultat {
        // Hent utbetalinger fra UtbetalingClient
        val utbetalingResponse = utbetalingClient.hentUtbetalingerForBruker(personIdent)
        logger.info("Hentet stønader for $personIdent, status ${utbetalingResponse.statusCode}")

        // Håndter feil fra UtbetalingClient
        when (utbetalingResponse.statusCode) {
            404 -> return StønadResultat.PersonIkkeFunnet
            403, 401 -> return StønadResultat.IngenTilgang
            500 -> return StønadResultat.FeilIBaksystem
            !in 200..299 -> return StønadResultat.FeilIBaksystem
        }

        // Håndter tom respons
        if (utbetalingResponse.data?.utbetalinger.isNullOrEmpty()) {
            logger.info("Fant ingen stønader for $personIdent")
            return StønadResultat.Success(emptyList())
        }

        // Mappe utbetalinger til stønader
        val utbetalinger = utbetalingResponse.data?.utbetalinger.orEmpty()
        var stønader: List<Stønad> = utbetalinger
            .asSequence()
            .flatMap { it.ytelseListe.asSequence() }
            .filter { it.ytelsestype != null }
            .groupBy { it.ytelsestype }
            .map { (type, liste) ->
                val perioder = liste.map { ytelse ->
                    Stønad.PeriodeInformasjon(
                        periode = Stønad.Periode(
                            fom = ytelse.ytelsesperiode.fom,
                            tom = ytelse.ytelsesperiode.tom
                        ),
                        beløp = ytelse.ytelseNettobeloep,
                        kilde = "SOKOS",
                        info = ytelse.bilagsnummer
                    )
                }
                Stønad(stonadType = type!!, perioder)
            }
            .toList()

        logger.info("Fant ${stønader.size} stønad(er) for $personIdent")

        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente stønader for $personIdent. Maskerer responsen")
            stønader = maskerObjekt(stønader)
        }

        return StønadResultat.Success(stønader)
    }
}

sealed class StønadResultat {
    data class Success(val data: List<Stønad>) : StønadResultat()
    data object IngenTilgang : StønadResultat()
    data object PersonIkkeFunnet : StønadResultat()
    data object FeilIBaksystem : StønadResultat()
}
