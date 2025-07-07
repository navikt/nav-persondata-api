package no.nav.persondataapi.service.dataproviders


import no.nav.persondataapi.service.GrunnlagsProvider
import no.nav.persondataapi.service.GrunnlagsType
import no.nav.persondataapi.service.GrunnlagsdelResultat
import no.nav.persondataapi.utbetaling.client.UtbetalingClient

import org.springframework.stereotype.Component

@Component
class UtbetalingGrunnlagsProvider(val utbetalingClient: UtbetalingClient) : GrunnlagsProvider {
    override val type = GrunnlagsType.UTBETALINGER

    override suspend fun hent(fnr: String, saksbehandlerId: String): GrunnlagsdelResultat {
        val resultat = utbetalingClient.hentUtbetalingerForAktor(fnr)
        return GrunnlagsdelResultat(
            type = type,
            data = resultat.data,
            ok = resultat.statusCode in 200..299,
            status = resultat.statusCode,
            feilmelding = resultat.errorMessage
        )
    }
}
