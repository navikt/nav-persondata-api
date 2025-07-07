package no.nav.persondataapi.service

import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.utbetaling.client.UtbetalingClient
import org.springframework.stereotype.Component

@Component
class OppslagService(val utbetalingClient: UtbetalingClient) {

    fun hentGrunnlagsData(fnr:String,saksbehandlerId:String): GrunnlagsData {
        val utbetalinger = utbetalingClient.hentUtbetalingerForAktor(fnr)
        return GrunnlagsData(ident = fnr,  utbetalingRespons = utbetalinger, saksbehandlerId = saksbehandlerId)
    }
}