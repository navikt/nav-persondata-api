package no.nav.persondataapi.domain

import no.nav.persondataapi.utbetaling.dto.Utbetaling
import java.time.ZonedDateTime

data class GrunnlagsData(
    val utreksTidspunkt: ZonedDateTime = ZonedDateTime.now(),
    val ident:String,
    val saksbehandlerId:String,
    val utbetalingRespons: UtbetalingRespons
)

data class UtbetalingRespons(val status:Boolean,val utbetalinger:List<Utbetaling>)