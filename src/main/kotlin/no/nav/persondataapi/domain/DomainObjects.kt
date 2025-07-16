package no.nav.persondataapi.domain

import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.service.dataproviders.GrunnlagsdelResultat
import no.nav.persondataapi.utbetaling.dto.Utbetaling
import java.time.ZonedDateTime

data class GrunnlagsData(
    val utreksTidspunkt: ZonedDateTime = ZonedDateTime.now(),
    val ident:String,
    val saksbehandlerId:String,
    val utbetalingRespons: GrunnlagsdelResultat?,
    val personDataRespons: GrunnlagsdelResultat?
)

data class UtbetalingRespons(val utbetalinger:List<Utbetaling>)

data class UtbetalingResultat(
    val data: UtbetalingRespons?,
    val statusCode: Int,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)

data class PersonDataResultat(
    val data: Person?,
    val statusCode: Int,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)