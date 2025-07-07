package no.nav.persondataapi.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.persondataapi.domain.GrunnlagsData

import no.nav.persondataapi.domain.UtbetalingResultat

import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OppslagService(
    private val providers: List<GrunnlagsProvider>  // injiseres automatisk av Spring
) {

    suspend fun hentGrunnlagsData(
        fnr: String,
        saksbehandlerId: String,
        typer: Set<GrunnlagsType> = GrunnlagsType.entries.toSet()
    ): GrunnlagsData {

        val resultater = coroutineScope {
            providers
                .filter { it.type in typer }
                .map { provider ->
                    async {
                        provider.hent(fnr, saksbehandlerId)
                    }
                }
                .awaitAll()
        }

        // Eksempel på hvordan du setter sammen full respons
        val utbetalinger = resultater
            .find { it.type == GrunnlagsType.UTBETALINGER }

        return GrunnlagsData(
            utreksTidspunkt = ZonedDateTime.now(),
            ident = fnr,
            saksbehandlerId = saksbehandlerId,
            utbetalingRespons = utbetalinger
        )
    }
}