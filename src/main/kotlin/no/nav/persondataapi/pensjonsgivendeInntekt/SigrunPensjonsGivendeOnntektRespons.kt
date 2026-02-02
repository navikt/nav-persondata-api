package no.nav.persondataapi.pensjonsgivendeInntekt

class SigrunPensjonsgivendeInntektResponse (
    val inntektsaar: String,
    val pensjonsgivendeInntekt: List<PensjonsgivendeInntekt>
)
data class PensjonsgivendeInntekt(
    val skatteordning: String,
    val datoForFastsetting: String,
    val pensjonsgivendeInntektAvLoennsinntekt: Int,
    val pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel: Int,
    val pensjonsgivendeInntektAvNaeringsinntekt: Int,
    val pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage: Int
)