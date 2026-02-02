package no.nav.persondataapi.service.domain.pensjonsgivendeinntekt

data class PensjonsgivendeInntekt(
   val innteksår:String,
   val skatteordning: Skatteordning
)

data class Skatteordning(
   val skatteordning: String,
   val datoForFastSetting:String,
   val inntektListe: List<Inntekt>
)

data class Inntekt(
   val type:InntektType,
   val belop:Double,
)

enum class InntektType {
   LØNN,
   LØNN_BARE_PERNSJON,
   NÆRING,
   NÆRING_FISKE_FANGST_BARNEHAGE
}

/*
*
* Fastland - Lønnsinntekt
Fastland - Lønnsinntekt - Bare pensjonsdel
Fastland - Næringsinntekt
Fastland - Næringsinntekt - Fiske, fangst eller familiebarnehage

Kildeskatt - Lønnsinntekt
Kildeskatt - Lønnsinntekt - Bare pensjonsdel
Kildeskatt - Næringsinntekt
Kildeskatt - Næringsinntekt - Fiske, fangst eller familiebarnehage

Svalbard - Lønnsinntekt
Svalbard - Lønnsinntekt - Bare pensjonsdel
Svalbard - Næringsinntekt
Svalbard - Næringsinntekt - Fiske, fangst eller familiebarnehage
*
* */