package no.nav.persondataapi.domain

import no.nav.inntekt.generated.model.InntektshistorikkApiUt

import no.nav.persondataapi.generated.hentperson.Person
import java.time.LocalDate


data class UtbetalingRespons(val utbetalinger:List<no.nav.persondataapi.integration.utbetaling.dto.Utbetaling>)

data class UtbetalingResultat(
    val data: UtbetalingRespons?,
    val statusCode: Int,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)

data class TilgangMaskinResultat(
    val type:String?,
    val title:String?,
    val status: Int,
    val instance: String?,
    val brukerIdent:String?,
    val navIdent:String?,
    val traceId:String?,
    val begrunnelse:String?,
    val kanOverstyres:Boolean?,
)


data class InntektDataResultat(
    val data: InntektshistorikkApiUt?,
    val statusCode: Int?,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)

data class AaregDataResultat(
    val data:  List<no.nav.persondataapi.integration.aareg.client.Arbeidsforhold> = emptyList(),
    val statusCode: Int?,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)
data class TilgangResultat(
    val data: TilgangMaskinResultat?,
    val statusCode: Int?,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)

data class PersonDataResultat(
    val data: Person?,
    val statusCode: Int,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)

data class KontrollPeriode(
    val fom: LocalDate,val tom: LocalDate )
