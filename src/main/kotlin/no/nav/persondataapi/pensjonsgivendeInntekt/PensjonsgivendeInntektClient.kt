package no.nav.persondataapi.pensjonsgivendeInntekt

import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.Inntekt
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.InntektType
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.PensjonsgivendeInntekt
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.Skatteordning
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class PensjonsgivendeInntektClient {

    @Cacheable(
        value = ["pensjonsgivende-inntekt"],
        key = "#personIdent + '_' + #utvidet",
        unless = "#result.statusCode != 200 && #result.statusCode != 404"
    )
    fun hentPensjonsgivendeInntekt(
        personIdent: PersonIdent,
        utvidet: Boolean,
    ): PensjonsgivendeInntektDataResultat {
        val antallÅr = if (utvidet) 6 else 3
        val inneværendeÅr = LocalDate.now().year

        val inntekter = (1..antallÅr).map { index ->
            val år = inneværendeÅr - index
            val beløp = BigDecimal(480000 + (index * 12000))

            PensjonsgivendeInntekt(
                innteksår = år.toString(),
                skatteordning = Skatteordning("FASTLAND",år.toString(),listOf(Inntekt(InntektType.LØNN,beløp.toDouble())))
            )
        }

        return PensjonsgivendeInntektDataResultat(
            data = PensjonsgivendeInntektRespons(inntekter = inntekter),
            statusCode = 200
        )
    }
}

data class PensjonsgivendeInntektRespons(
    val inntekter: List<PensjonsgivendeInntekt>
)



data class PensjonsgivendeInntektDataResultat(
    val data: PensjonsgivendeInntektRespons?,
    val statusCode: Int,
    val errorMessage: String? = null,
)
