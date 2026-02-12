package no.nav.persondataapi.pensjonsgivendeInntekt

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import no.nav.persondataapi.service.BrukertilgangService
import no.nav.persondataapi.tracelogging.traceLoggHvisAktivert

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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

        val `antallÅr` = if (utvidet) 10 else 3
        val `årListe` = `HistoriskeÅrService`().`hentTidligereÅrEkskludertNåværende`(`antallÅr`)
        val deferred = `årListe`.map { aar ->
            async {
                `hentPenjonsgivendeInntektForÅr`(personIdent, aar)
            }
        }

        var resultater = deferred.awaitAll()
        traceLoggHvisAktivert(
            logger = logger,
            personIdent = personIdent,
            kilde = "Sigrun",
            unit = resultater
        )
        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente pensjonsgivende inntekt for $personIdent. Maskerer responsen")
            resultater = maskerObjekt(resultater)
        }
        
        val gyldige = resultater
            .filterIsInstance<AarsResultat.Ok>()
            .map { it.data }

        val oppsummering = gyldige.toPensjonsgivendeInntektOppummering()

        if (gyldige.isEmpty()) {
            PensjonsgivendeInntektResultat.PersonIkkeFunnet
        } else {
            PensjonsgivendeInntektResultat.Success(oppsummering)
        }
    }

    sealed class AarsResultat {
        data class Ok(val data: SigrunPensjonsgivendeInntektResponse) : AarsResultat()
        data object IkkeFunnet : AarsResultat()
        data object FeilIBaksystem : AarsResultat()
    }

    sealed class PensjonsgivendeInntektResultat {
        data class Success(val data: List<PensjonsGivendeInntektOppummering>) : PensjonsgivendeInntektResultat()
        data object IngenTilgang : PensjonsgivendeInntektResultat()
        data object PersonIkkeFunnet : PensjonsgivendeInntektResultat()
        data object FeilIBaksystem : PensjonsgivendeInntektResultat()
    }
}

data class PensjonsGivendeInntektOppummering(
    val `inntektsår`: String,
    val næringsinntekt: Int =0,
    val lønnsinntekt: Int =0,
)

