package no.nav.persondataapi.pensjonsgivendeInntekt

import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import no.nav.persondataapi.service.BrukertilgangService
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.PensjonsgivendeInntekt
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.Skatteordning
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PensjonsgivendeInntektService(
    private val pensjonsgivendeInntektClient: PensjonsgivendeInntektClient,
    private val brukertilgangService: BrukertilgangService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentPensjonsgivendeInntektForPerson(
        personIdent: PersonIdent,
        utvidet: Boolean = false,
    ): PensjonsgivendeInntektResultat {
        val respons = pensjonsgivendeInntektClient.hentPensjonsgivendeInntekt(personIdent, utvidet)
        logger.info("Hentet pensjonsgivende inntekt for $personIdent (utvidet = $utvidet), status ${respons.statusCode}")

        when (respons.statusCode) {
            404 -> return PensjonsgivendeInntektResultat.PersonIkkeFunnet
            403 -> return PensjonsgivendeInntektResultat.IngenTilgang
            500 -> return PensjonsgivendeInntektResultat.FeilIBaksystem
            !in 200..299 -> return PensjonsgivendeInntektResultat.FeilIBaksystem
        }


        /*
        * DO MAPPING HERE IF NEEDED
        * */
        var resultat = respons
        val v = PensjonsgivendeInntekt("2014", skatteordning = Skatteordning("","2016",emptyList()))



        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente pensjonsgivende inntekt for $personIdent. Maskerer responsen")
            resultat = maskerObjekt(resultat)
        }

        return PensjonsgivendeInntektResultat.Success(resultat.data!!.inntekter)
    }
}

sealed class PensjonsgivendeInntektResultat {
    data class Success(val data: List<PensjonsgivendeInntekt>) : PensjonsgivendeInntektResultat()
    data object IngenTilgang : PensjonsgivendeInntektResultat()
    data object PersonIkkeFunnet : PensjonsgivendeInntektResultat()
    data object FeilIBaksystem : PensjonsgivendeInntektResultat()
}
