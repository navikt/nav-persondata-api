package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.dagpenger.datadeling.dagpengerDatadelingClient
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.Meldekort
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MeldekortService(
    private val dpDatadelingClient: dagpengerDatadelingClient,
    private val brukertilgangService: BrukertilgangService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentDagpengeMeldekortForPerson(personIdent: PersonIdent, utvidet: Boolean): MeldekortResultat {
        val utbetalingResponse = dpDatadelingClient.hentInntekter(personIdent, utvidet)
        logger.info("Hentet ${if (utvidet) "utvidete " else ""}dagpenger meldekort for $personIdent, status ${utbetalingResponse.statusCode}")

        when (utbetalingResponse.statusCode) {
            404 -> return MeldekortResultat.PersonIkkeFunnet
            403, 401 -> return MeldekortResultat.IngenTilgang
            500 -> return MeldekortResultat.FeilIBaksystem
            !in 200..299 -> return MeldekortResultat.FeilIBaksystem
        }

        if (utbetalingResponse.data.isNullOrEmpty()) {
            logger.info("Fant ingen dagpenge meldekort for $personIdent")
            return MeldekortResultat.Success(emptyList())
        }

        var meldekort = utbetalingResponse.data


        logger.info("Fant ${meldekort.size} meldekort for $personIdent")

        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente meldekort for $personIdent. Maskerer responsen")
            meldekort = maskerObjekt(meldekort)

        }

        return MeldekortResultat.Success(meldekort)
    }
}

sealed class MeldekortResultat {
    data class Success(val data: List<Meldekort>) : MeldekortResultat()
    data object IngenTilgang : MeldekortResultat()
    data object PersonIkkeFunnet : MeldekortResultat()
    data object FeilIBaksystem : MeldekortResultat()
}
