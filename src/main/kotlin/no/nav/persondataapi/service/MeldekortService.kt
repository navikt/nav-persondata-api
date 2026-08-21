package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.aap.meldekort.client.AapClient
import no.nav.persondataapi.integrasjon.aap.meldekort.client.alleTimerArbeidSegmenter
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.HolmesTimerArbeid
import no.nav.persondataapi.integrasjon.dagpenger.datadeling.DagpengerDatadelingClient
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.MeldekortStatus
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.timerAsDouble
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import no.nav.persondataapi.service.domain.AapMeldekortDto
import no.nav.persondataapi.service.domain.AapMeldekortPeriode
import no.nav.persondataapi.service.domain.AktivitetDto
import no.nav.persondataapi.service.domain.AktivitetTypeDto
import no.nav.persondataapi.service.domain.DagpengerMeldekortDag
import no.nav.persondataapi.service.domain.DagpengerMeldekortDto
import no.nav.persondataapi.service.domain.KildeDto
import no.nav.persondataapi.service.domain.PeriodeDto
import no.nav.persondataapi.service.domain.ÅpenPeriode
import no.nav.persondataapi.tracelogging.traceLoggHvisAktivert
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class MeldekortService(
    private val dpDatadelingClient: DagpengerDatadelingClient,
    private val aapClient: AapClient,
    private val brukertilgangService: BrukertilgangService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentDagpengeMeldekortForPerson(
        personIdent: PersonIdent,
        utvidet: Boolean,
    ): MeldekortResultat {
        val meldekortRespons = dpDatadelingClient.hentDagpengeMeldekort(personIdent, utvidet)
        logger.info(
            "Hentet ${if (utvidet) "utvidet " else ""} dagpenger-meldekort for $personIdent, status ${meldekortRespons.statusCode}",
        )
        traceLoggHvisAktivert(
            logger = logger,
            kilde = "Dagpenger",
            personIdent = personIdent,
            unit = meldekortRespons,
        )

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
        logger.info("Fant ${meldekort.size} meldekort for $personIdent hvorav $antallInnsendt har status Innsendt")
        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente meldekort for $personIdent. Maskerer responsen")
            meldekort = maskerObjekt(meldekort)
        }

        val response =
            meldekort
                .filter { meldekort -> meldekort.status == MeldekortStatus.Innsendt }
                .map { meldekort ->
                    DagpengerMeldekortDto(
                        dager =
                            meldekort.dager.map { dag ->
                                DagpengerMeldekortDag(
                                    dato = dag.dato,
                                    aktiviteter =
                                        dag.aktiviteter.map { aktivitet ->
                                            AktivitetDto(
                                                id = aktivitet.id,
                                                type = AktivitetTypeDto.valueOf(aktivitet.type.name),
                                                timer = aktivitet.timerAsDouble(),
                                                dato = dag.dato,
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

    fun hentAAPMeldekortForPerson(
        personIdent: PersonIdent,
        utvidet: Boolean,
    ): AAPMeldekortResultat {
        val meldekortRespons = aapClient.hentAapMax(personIdent, utvidet)
        logger.info(
            "Hentet ${if (utvidet) "utvidete " else ""} AAP-meldekort for $personIdent, status ${meldekortRespons.statusCode}",
        )

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
        logger.info(
            "Fant ${meldekort.size} aap meldekort (vedtak) for $personIdent",
            "hvorav $antallInnsendt har status Innsendt",
        )
        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente meldekort for $personIdent. Maskerer responsen")
            meldekort = maskerObjekt(meldekort)
        }

        // `/maksimum` sender alltid reduksjon=null for Kelvin-vedtak (se
        // https://github.com/navikt/aap-api-intern/issues/929), så faktiske
        // arbeidstimer må hentes separat fra det dedikerte Holmes-endepunktet
        // og kobles sammen med utbetalingsperiodene under. Feiler kallet,
        // fortsetter vi uten arbeidstimer (arbeidetTimer blir null/reduksjon-
        // fallback) i stedet for å la hele oppslaget feile.
        val timerArbeidSegmenter =
            aapClient
                .hentArbeidstimer(personIdent, utvidet)
                ?.alleTimerArbeidSegmenter()
                ?: emptyList()

        val nyModell =
            meldekort.map { aapvedtak ->
                AapMeldekortDto(
                    vedtakId = aapvedtak.vedtakId,
                    status = aapvedtak.status,
                    saksnummer = aapvedtak.saksnummer,
                    vedtakPeriode = ÅpenPeriode(aapvedtak.periode.fraOgMedDato, aapvedtak.periode.tilOgMedDato),
                    rettighetsType = aapvedtak.rettighetsType,
                    kide = aapvedtak.kildesystem,
                    tema = Tema.AAP,
                    vedtaktypeNavn = aapvedtak.vedtaksTypeNavn,
                    perioder =
                        aapvedtak.utbetaling.map { utbetaling ->
                            // tilOgMedDato null betyr en ÅPEN periode (løper fra fraOgMedDato
                            // og videre uten kjent sluttdato). Klipp til i dag som foreløpig
                            // grense når vi beregner overlapp mot Holmes-segmentene under —
                            // selve perioden sendes fortsatt uendret (null) videre til frontend.
                            val tilOgMedEllerIDag = utbetaling.periode.tilOgMedDato ?: LocalDate.now()

                            val arbeidetTimer =
                                beregnArbeidetTimerFraHolmesSegmenter(
                                    periodeFom = utbetaling.periode.fraOgMedDato,
                                    periodeTom = tilOgMedEllerIDag,
                                    segmenter = timerArbeidSegmenter,
                                ) ?: utbetaling.reduksjon?.timerArbeidet

                            val annenReduksjon = utbetaling.reduksjon?.annenReduksjon
                            val utbetalingsgrad = utbetaling.utbetalingsgrad

                            AapMeldekortPeriode(
                                fraOgMed = utbetaling.periode.fraOgMedDato,
                                tilOgMed = utbetaling.periode.tilOgMedDato,
                                arbeidetTimer = arbeidetTimer,
                                annenReduksjon = annenReduksjon,
                                utbetalingsgrad = utbetalingsgrad,
                            )
                        },
                )
            }
        return AAPMeldekortResultat.Success(nyModell)
    }
}

/**
 * Beregner arbeidstimer for en gitt periode ved å summere overlappende
 * `timerArbeid`-segmenter fra Holmes-endepunktet, pro-ratert etter antall
 * overlappende dager. Nødvendig fordi meldeperiodene (alltid 2 uker) fra
 * `/holmes/arbeidstimer` ikke nødvendigvis følger samme periodeinndeling
 * som utbetalingsperiodene fra `/maksimum` (som kan splittes av bl.a.
 * G-regulering eller endret rettighetsType). MERK: dette gjør at verdien
 * for en splittet periode er et day-vektet ESTIMAT, ikke et tall hentet
 * direkte fra kildedataen — se diskusjon i PR-beskrivelsen.
 *
 * Regner i BigDecimal helt til slutt for å unngå avrundingsstøy fra Double
 * ved gjentatte delberegninger; kun sluttresultatet konverteres til Double
 * siden det er typen [no.nav.persondataapi.service.domain.AapMeldekortPeriode.arbeidetTimer]
 * bruker videre til frontend.
 *
 * Returnerer `null` (ikke 0.0) dersom ingen segmenter overlapper perioden,
 * slik at kalleren kan falle tilbake på annen datakilde eller la verdien
 * forbli ukjent — 0.0 ville feilaktig indikert "bekreftet ingen arbeid".
 */
internal fun beregnArbeidetTimerFraHolmesSegmenter(
    periodeFom: LocalDate,
    periodeTom: LocalDate,
    segmenter: List<HolmesTimerArbeid>,
): Double? {
    var totalTimer = BigDecimal.ZERO
    var harOverlapp = false

    for (segment in segmenter) {
        val effektivFom = maxOf(segment.periodeFom, periodeFom)
        val effektivTom = minOf(segment.periodeTom, periodeTom)
        if (effektivFom.isAfter(effektivTom)) continue

        val totalDagerISegment = ChronoUnit.DAYS.between(segment.periodeFom, segment.periodeTom) + 1
        if (totalDagerISegment <= 0) continue

        val overlappendeDager = ChronoUnit.DAYS.between(effektivFom, effektivTom) + 1

        harOverlapp = true
        val andel =
            BigDecimal(overlappendeDager).divide(BigDecimal(totalDagerISegment), 10, RoundingMode.HALF_UP)
        totalTimer = totalTimer.add(segment.timerArbeidet.multiply(andel))
    }

    return if (harOverlapp) totalTimer.setScale(2, RoundingMode.HALF_UP).toDouble() else null
}

enum class Tema {
    AAP,
    DAG,
    TILTAK,
}

sealed class MeldekortResultat {
    data class Success(
        val data: List<DagpengerMeldekortDto>,
    ) : MeldekortResultat()

    data object IngenTilgang : MeldekortResultat()

    data object PersonIkkeFunnet : MeldekortResultat()

    data object FeilIBaksystem : MeldekortResultat()
}

sealed class AAPMeldekortResultat {
    data class Success(
        val data: List<AapMeldekortDto>,
    ) : AAPMeldekortResultat()

    data object IngenTilgang : AAPMeldekortResultat()

    data object PersonIkkeFunnet : AAPMeldekortResultat()

    data object FeilIBaksystem : AAPMeldekortResultat()
}
