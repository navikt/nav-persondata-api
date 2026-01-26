package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.aap.meldekort.client.AapClient
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.Vedtak
import no.nav.persondataapi.integrasjon.dagpenger.datadeling.DagpengerDatadelingClient
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.MeldekortStatus
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.timerAsDouble
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class MeldekortService(
    private val dpDatadelingClient: DagpengerDatadelingClient,
    private val aapClient: AapClient,
    private val brukertilgangService: BrukertilgangService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentDagpengeMeldekortForPerson(personIdent: PersonIdent, utvidet: Boolean): MeldekortResultat {
        val meldekortRespons = dpDatadelingClient.hentDagpengeMeldekort(personIdent, utvidet)
        logger.info("Hentet ${if (utvidet) "utvidete " else ""} dagpenge-meldekort for $personIdent, status ${meldekortRespons.statusCode}")

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
                MeldekortDto(
                    dager = meldekort.dager.map { dag ->
                        DagDto(
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
                    migrert = meldekort.migrert,
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
            AAPMeldekortDto(
                vedtakId = aapvedtak.vedtakId,
                status = aapvedtak.status,
                saksnummer = aapvedtak.saksnummer,
                vedtakPeriode = ÅpenPeriode(aapvedtak.periode.fraOgMedDato, aapvedtak.periode.tilOgMedDato),
                rettighetsType = aapvedtak.rettighetsType,
                kide = aapvedtak.kildesystem,
                tema = Tema.AAP,
                perioder = aapvedtak.utbetaling.map {
                    utbetaling ->
                    val arbeidetTimer = utbetaling.reduksjon?.timerArbeidet
                    val annenReduksjon = utbetaling.reduksjon?.annenReduksjon
                    val utbetalingsgrad = utbetaling.utbetalingsgrad

                    MeldekortPeriode(
                        fraOgMed = utbetaling.periode.fraOgMedDato,
                        tilOgMed = utbetaling.periode.tilOgMedDato!!,
                        arbeidetTimer = arbeidetTimer,
                        annenReduksjon = annenReduksjon,
                        utbetalingsgrad = utbetalingsgrad,
                    )
                }
            )
        }
        return AAPMeldekortResultat.Success(meldekortRespons.data)
    }
}

data class AAPMeldekortDto(
    val vedtakId: String,
    val status: String,
    val saksnummer: String,
    val vedtakPeriode: ÅpenPeriode,
    val rettighetsType: String,
    val kide:String,
    val tema:Tema,
    val perioder:List<MeldekortPeriode> = emptyList(),
)

data class MeldekortPeriode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val arbeidetTimer:String?,
    val annenReduksjon:String?,
    val utbetalingsgrad:Int?

)
data class TimerDto(
    val antallTimer: String?,
    val grad:Int?,
    val type: AktivitetTypeDto
)

enum class Tema {
    AAP, DAG, TILTAK
}

data class MeldekortDto(
    val dager: List<DagDto>,
    val id: String,
    val periode: PeriodeDto,
    val opprettetAv: String,
    val migrert: Boolean,
    val kilde: KildeDto,
    val innsendtTidspunkt: LocalDateTime?,
    val registrertArbeidssoker: Boolean?,
    val meldedato: LocalDate?
)

data class ÅpenPeriode(
    val fraOgMed: LocalDate, val tilOgMed: LocalDate?
)

data class PeriodeDto(
    val fraOgMed: LocalDate, val tilOgMed: LocalDate
)

data class KildeDto(
    val rolle: String, val ident: String
)

data class DagDto(
    val dato: LocalDate, val aktiviteter: List<AktivitetDto>, val dagIndex: Int
)

data class AktivitetDto(
    val id: String,
    val type: AktivitetTypeDto,
    val timer: Double?,
    val dato: LocalDate?
)

enum class AktivitetTypeDto {
    Arbeid, Fravaer, Syk, Utdanning,Annet
}

sealed class MeldekortResultat {
    data class Success(val data: List<MeldekortDto>) : MeldekortResultat()
    data object IngenTilgang : MeldekortResultat()
    data object PersonIkkeFunnet : MeldekortResultat()
    data object FeilIBaksystem : MeldekortResultat()
}

sealed class AAPMeldekortResultat {
    data class Success(val data: List<Vedtak>) : AAPMeldekortResultat()
    data object IngenTilgang : AAPMeldekortResultat()
    data object PersonIkkeFunnet : AAPMeldekortResultat()
    data object FeilIBaksystem : AAPMeldekortResultat()
}
