package no.nav.persondataapi.integrasjon.aap.meldekort.domene

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Request-body for `/holmes/arbeidstimer` i aap-api-intern. Dette endepunktet
 * er laget spesifikt for oss (team Holmes) — se [HolmesArbeidstimerRespons]
 * for bakgrunn. Feltnavnet `personidentifikator` (ikke `personIdent`) må
 * matche kontrakten i aap-api-intern nøyaktig.
 */
data class HolmesArbeidstimerRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
)

/**
 * Respons fra `/holmes/arbeidstimer`. Endepunktet ble laget etter at vi
 * oppdaget at `utbetaling.reduksjon` (og dermed `timerArbeidet`) alltid er
 * `null` for Kelvin-vedtak i `/maksimum` — se AAP_SCOPE-integrasjonen i
 * [no.nav.persondataapi.integrasjon.aap.meldekort.client.AapClient] for
 * hvordan dette kombineres med `/maksimum`-responsen.
 */
data class HolmesArbeidstimerRespons(
    val personIdent: String,
    val meldeperioder: List<HolmesMeldeperiode>,
)

data class HolmesMeldeperiode(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val timerArbeid: List<HolmesTimerArbeid>,
)

data class HolmesTimerArbeid(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val timerArbeidet: BigDecimal,
)
