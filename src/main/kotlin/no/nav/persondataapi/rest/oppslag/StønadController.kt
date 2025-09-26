package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.rest.domain.Periode
import no.nav.persondataapi.rest.domain.PeriodeInformasjon
import no.nav.persondataapi.rest.domain.Stonad
import no.nav.persondataapi.service.BrukertilgangService
import no.nav.persondataapi.utbetaling.client.UtbetalingClient
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import kotlin.collections.emptyList

@Controller("/oppslag")
class StønadController(
    val utbetalingClient: UtbetalingClient,
    val brukertilgangService: BrukertilgangService
) {
    @Protected
    @PostMapping("/stønad")
    fun hentStønader(@RequestBody dto: OppslagRequestDto): ResponseEntity<OppslagResponseDto<List<Stonad>>> {
        return runBlocking {
            if (!brukertilgangService.harBrukerTilgangTilIdent(dto.ident)) {
                ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
            }
            val utbetalingResponse = utbetalingClient.hentUtbetalingerForAktor(dto.ident)

            when (utbetalingResponse.statusCode) {
                404 -> ResponseEntity(OppslagResponseDto(error = "Person ikke funnet", data = null), HttpStatus.NOT_FOUND)
                403 -> ResponseEntity(OppslagResponseDto(error = "Ingen tilgang", data = null), HttpStatus.FORBIDDEN)
                500 -> ResponseEntity(OppslagResponseDto(error = "Feil i baksystem", data = null), HttpStatus.BAD_GATEWAY)
            }

            if (utbetalingResponse.data?.utbetalinger.isNullOrEmpty()) {
                ResponseEntity.ok(OppslagResponseDto(data = emptyList<Stonad>()))
            }

            val utbetalinger = utbetalingResponse.data?.utbetalinger.orEmpty()
            val stønader: List<Stonad> =
                utbetalinger
                    .asSequence()
                    .flatMap { it.ytelseListe.asSequence() }
                    .filter { it.ytelsestype != null }
                    .groupBy { it.ytelsestype }
                    .map { (type, liste) ->
                        val perioder = liste.map { y ->
                            PeriodeInformasjon(
                                periode = Periode(fom = y.ytelsesperiode.fom, tom = y.ytelsesperiode.tom),
                                beløp = y.ytelseNettobeloep,
                                kilde = "SOKOS",
                                info = y.bilagsnummer
                            )
                        }
                        Stonad(stonadType = type!!, perioder)
                    }
                    .toList()
            ResponseEntity.ok(
                OppslagResponseDto(
                    data = stønader
                )
            )
        }
    }
}
