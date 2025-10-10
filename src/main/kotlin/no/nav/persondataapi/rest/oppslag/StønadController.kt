package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.rest.domene.Stønad
import no.nav.persondataapi.service.BrukertilgangService
import no.nav.persondataapi.integrasjon.utbetaling.client.UtbetalingClient
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import kotlin.time.measureTimedValue

@Controller
@RequestMapping("/oppslag/stønad")
class StønadController(
    val utbetalingClient: UtbetalingClient,
    val brukertilgangService: BrukertilgangService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)

    @Protected
    @PostMapping
    fun hentStønader(@RequestBody dto: OppslagRequestDto): ResponseEntity<OppslagResponseDto<List<Stønad>>> {
        return runBlocking {
            if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(dto.ident.value)) {
                logger.info("Saksbehandler har ikke tilgang til å hente stønader for ${dto.ident}")
                ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
            }
            val (utbetalingResponse, tid) = measureTimedValue {
                utbetalingClient.hentUtbetalingerForBruker(dto.ident.value)
            }
            logger.info("Hentet stønader for ${dto.ident} på ${tid.inWholeMilliseconds} ms, fikk status ${utbetalingResponse.statusCode}")

            when (utbetalingResponse.statusCode) {
                404 -> ResponseEntity(OppslagResponseDto(error = "Person ikke funnet", data = null), HttpStatus.NOT_FOUND)
                403 -> ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
                500 -> ResponseEntity(OppslagResponseDto(error = "Feil i baksystem", data = null), HttpStatus.BAD_GATEWAY)
            }

            if (utbetalingResponse.data?.utbetalinger.isNullOrEmpty()) {
                logger.info("Fant ingen stønader for ${dto.ident}")
                ResponseEntity.ok(OppslagResponseDto(data = emptyList<Stønad>()))
            }

            val utbetalinger = utbetalingResponse.data?.utbetalinger.orEmpty()
            val stønader: List<Stønad> =
                utbetalinger
                    .asSequence()
                    .flatMap { it.ytelseListe.asSequence() }
                    .filter { it.ytelsestype != null }
                    .groupBy { it.ytelsestype }
                    .map { (type, liste) ->
                        val perioder = liste.map { y ->
                            Stønad.PeriodeInformasjon(
                                periode = Stønad.Periode(fom = y.ytelsesperiode.fom, tom = y.ytelsesperiode.tom),
                                beløp = y.ytelseNettobeloep,
                                kilde = "SOKOS",
                                info = y.bilagsnummer
                            )
                        }
                        Stønad(stonadType = type!!, perioder)
                    }
                    .toList()
            logger.info("Fant ${stønader.size} stønad(er) for ${dto.ident}")
            ResponseEntity.ok(
                OppslagResponseDto(
                    data = stønader
                )
            )
        }
    }
}
