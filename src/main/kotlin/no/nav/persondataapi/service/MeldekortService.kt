package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.aap.meldekort.client.AapClient
import no.nav.persondataapi.integrasjon.dagpenger.datadeling.DagpengerDatadelingClient
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.MeldekortStatus
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.timerAsDouble
import no.nav.persondataapi.responstracing.erTraceLoggingAktvert
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import no.nav.persondataapi.service.domain.AapMeldekortDto
import no.nav.persondataapi.service.domain.AktivitetDto
import no.nav.persondataapi.service.domain.AktivitetTypeDto
import no.nav.persondataapi.service.domain.DagpengerMeldekortDag
import no.nav.persondataapi.service.domain.DagpengerMeldekortDto
import no.nav.persondataapi.service.domain.KildeDto
import no.nav.persondataapi.service.domain.AapMeldekortPeriode
import no.nav.persondataapi.service.domain.PeriodeDto
import no.nav.persondataapi.service.domain.ÅpenPeriode
import no.nav.persondataapi.tracelogging.traceLogg
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MeldekortService(
    private val dpDatadelingClient: DagpengerDatadelingClient,
    private val aapClient: AapClient,
    private val brukertilgangService: BrukertilgangService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentDagpengeMeldekortForPerson(personIdent: PersonIdent, utvidet: Boolean): MeldekortResultat {
        val meldekortRespons = dpDatadelingClient.hentDagpengeMeldekort(personIdent, utvidet)
        logger.info("Hentet ${if (utvidet) "utvidet " else ""} dagpenger-meldekort for $personIdent, status ${meldekortRespons.statusCode}")
        if (erTraceLoggingAktvert()){
            traceLogg(
                logger = logger,
                kilde = "Dagpenger",
                personIdent=personIdent,
                unit = meldekortRespons
            )
        }
        when (meldekortRespons.statusCode) {
            404 -> return MeldekortResultat.PersonIkkeFunnet
            403, 401 -> return MeldekortResultat.IngenTilgang
            500 -> return MeldekortResultat.FeilIBaksystem
            !in 200..299 -> return MeldekortResultat.FeilIBaksystem
        }

        if (meldekortRespons.data.isNullOrEmpty()) {
            logger.info("Fant ingen dagpenge-meldekort for $personIdent")
            return MeldekortResultat.Success(emptyList())
        }

        var meldekort = meldekortRespons.data
        val antallInnsendt = meldekort.filter { meldekort -> meldekort.status == MeldekortStatus.Innsendt }.size
        logger.info("Fant ${meldekort.size} meldekort for $personIdent", "hvorav $antallInnsendt har status Innsendt")
        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente meldekort for $personIdent. Maskerer responsen")
            meldekort = maskerObjekt(meldekort)
        }

        val response = meldekort
            .filter { meldekort -> meldekort.status == MeldekortStatus.Innsendt }
            .map { meldekort ->
                DagpengerMeldekortDto(
                    dager = meldekort.dager.map { dag ->
                        DagpengerMeldekortDag(
                            dato = dag.dato,
                            aktiviteter = dag.aktiviteter.map { aktivitet ->
                                AktivitetDto(
                                    id = aktivitet.id,
                                    type = AktivitetTypeDto.valueOf(aktivitet.type.name),
                                    timer = aktivitet.timerAsDouble(),
                                    dato = dag.dato
                                )
                            },
                            dagIndex = dag.dagIndex,

                            )
                    },
                    periode = PeriodeDto(meldekort.periode.fraOgMed, meldekort.periode.tilOgMed),
                    opprettetAv = meldekort.opprettetAv,
                    migrert = false,
                    kilde = KildeDto(meldekort.kilde.rolle, meldekort.kilde.ident),
                    innsendtTidspunkt = meldekort.innsendtTidspunkt,
                    registrertArbeidssoker = meldekort.registrertArbeidssoker,
                    meldedato = meldekort.meldedato,
                    id = meldekort.id,
                )
            }
        return MeldekortResultat.Success(response)
    }

    fun hentAAPMeldekortForPerson(personIdent: PersonIdent, utvidet: Boolean): AAPMeldekortResultat {
        val meldekortRespons = aapClient.hentAapMax(personIdent, utvidet)
        logger.info("Hentet ${if (utvidet) "utvidete " else ""} AAP-meldekort for $personIdent, status ${meldekortRespons.statusCode}")

        when (meldekortRespons.statusCode) {
            404 -> return AAPMeldekortResultat.PersonIkkeFunnet
            403, 401 -> return AAPMeldekortResultat.IngenTilgang
            500 -> return AAPMeldekortResultat.FeilIBaksystem
            !in 200..299 -> return AAPMeldekortResultat.FeilIBaksystem
        }

        if (meldekortRespons.data.isNullOrEmpty()) {
            logger.info("Fant ingen dagpenge-meldekort for $personIdent")
            return AAPMeldekortResultat.Success(emptyList())
        }

        var meldekort = meldekortRespons.data
        val antallInnsendt = meldekort.size
        logger.info("Fant ${meldekort.size} aap meldekort (vedtak) for $personIdent", "hvorav $antallInnsendt har status Innsendt")
        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente meldekort for $personIdent. Maskerer responsen")
            meldekort = maskerObjekt(meldekort)
        }
        val new_modell = meldekort.map {
            aapvedtak ->
            AapMeldekortDto(
                vedtakId = aapvedtak.vedtakId,
                status = aapvedtak.status,
                saksnummer = aapvedtak.saksnummer,
                vedtakPeriode = ÅpenPeriode(aapvedtak.periode.fraOgMedDato, aapvedtak.periode.tilOgMedDato),
                rettighetsType = aapvedtak.rettighetsType,
                kide = aapvedtak.kildesystem,
                tema = Tema.AAP,
                vedtaktypeNavn = aapvedtak.vedtaksTypeNavn,
                perioder = aapvedtak.utbetaling.map { utbetaling ->
                    val arbeidetTimer = utbetaling.reduksjon?.timerArbeidet
                    val annenReduksjon = utbetaling.reduksjon?.annenReduksjon
                    val utbetalingsgrad = utbetaling.utbetalingsgrad

                    AapMeldekortPeriode(
                        fraOgMed = utbetaling.periode.fraOgMedDato,
                        tilOgMed = utbetaling.periode.tilOgMedDato!!,
                        arbeidetTimer = arbeidetTimer,
                        annenReduksjon = annenReduksjon,
                        utbetalingsgrad = utbetalingsgrad,
                    )
                }
            )
        }
        return AAPMeldekortResultat.Success(new_modell)
    }
}




enum class Tema {
    AAP, DAG, TILTAK
}



sealed class MeldekortResultat {
    data class Success(val data: List<DagpengerMeldekortDto>) : MeldekortResultat()
    data object IngenTilgang : MeldekortResultat()
    data object PersonIkkeFunnet : MeldekortResultat()
    data object FeilIBaksystem : MeldekortResultat()
}

sealed class AAPMeldekortResultat {
    data class Success(val data: List<AapMeldekortDto>) : AAPMeldekortResultat()
    data object IngenTilgang : AAPMeldekortResultat()
    data object PersonIkkeFunnet : AAPMeldekortResultat()
    data object FeilIBaksystem : AAPMeldekortResultat()
}
