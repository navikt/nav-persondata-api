package no.nav.persondataapi.pensjonsgivendeInntekt

fun List<SigrunPensjonsgivendeInntektResponse>.toPensjonsgivendeInntektOppummering():
        List<PensjonsGivendeInntektOppummering> {

    return this
        .groupBy { it.inntektsaar }
        .map { (inntektsaar, responsesForAar) ->

            val alleInntekter = responsesForAar
                .flatMap { it.pensjonsgivendeInntekt }

            val lonnsinntekt = alleInntekter.sumOf {
                it.pensjonsgivendeInntektAvLoennsinntekt +
                        it.pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel
            }

            val naeringsinntekt = alleInntekter.sumOf {
                it.pensjonsgivendeInntektAvNaeringsinntekt +
                        it.pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage
            }

            PensjonsGivendeInntektOppummering(
                `inntektsår` = inntektsaar,
                lønnsinntekt = lonnsinntekt,
                næringsinntekt = naeringsinntekt
            )
        }
}
