package no.nav.persondataapi.pensjonsgivendeInntekt

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import no.nav.persondataapi.service.BrukertilgangService

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Year

@Service
class PensjonsgivendeInntektService(
    private val pensjonsgivendeInntektClient: PensjonsgivendeInntektClient,
    private val brukertilgangService: BrukertilgangService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun `hentPenjonsgivendeInntektForÅr`(
        personIdent: PersonIdent,
        aar: Int,

    ): AarsResultat {
        val respons = pensjonsgivendeInntektClient
            .hentPensjonsgivendeInntekt(personIdent, aar)

        return when (respons.statusCode) {
            200 -> AarsResultat.Ok(respons.data!!)
            404 -> AarsResultat.IkkeFunnet
            in 500..599 -> AarsResultat.FeilIBaksystem
            else -> AarsResultat.FeilIBaksystem
        }
    }
    suspend fun hentPensjonsgivendeInntektForPerson(
        personIdent: PersonIdent,
        utvidet: Boolean = false,
    ): PensjonsgivendeInntektResultat = coroutineScope {

        val antallAar = if (utvidet) 10 else 3
        val aarListe = (0 until antallAar).map { Year.now().value - it }

        val deferred = aarListe.map { aar ->
            async {
                `hentPenjonsgivendeInntektForÅr`(personIdent, aar)
            }
        }

        var resultater = deferred.awaitAll()
        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente pensjonsgivende inntekt for $personIdent. Maskerer responsen")
            resultater = maskerObjekt(resultater)
        }
        
        val gyldige = resultater
            .filterIsInstance<AarsResultat.Ok>()
            .map { it.data }

        if (gyldige.isEmpty()) {
            PensjonsgivendeInntektResultat.PersonIkkeFunnet
        } else {
            PensjonsgivendeInntektResultat.Success(gyldige)
        }
    }

    sealed class AarsResultat {
        data class Ok(val data: SigrunPensjonsgivendeInntektResponse) : AarsResultat()
        data object IkkeFunnet : AarsResultat()
        data object FeilIBaksystem : AarsResultat()
    }

    sealed class PensjonsgivendeInntektResultat {
        data class Success(val data: List<SigrunPensjonsgivendeInntektResponse>) : PensjonsgivendeInntektResultat()
        data object IngenTilgang : PensjonsgivendeInntektResultat()
        data object PersonIkkeFunnet : PensjonsgivendeInntektResultat()
        data object FeilIBaksystem : PensjonsgivendeInntektResultat()
    }
}

