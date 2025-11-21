package no.nav.persondataapi.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer

enum class DownstreamResult {
    SUCCESS,
    TIMEOUT,
    CLIENT_ERROR,
    SERVER_ERROR,
    UNEXPECTED
}

/**
 * Felles logikk for nedstrøms-metrikker.
 *
 * Standardiserte metrikknavn:
 *  - downstream_tid_seconds_*   (Timer)
 *  - downstream_kall_total      (Counter)
 *
 * Standard tags:
 *  - system   = pdl / utbetaling / inntekt / aareg / ereg / norg2
 *  - operation = funksjon / API-operasjon (f.eks. hentUtbetalinger)
 *  - result   = success / timeout / client_error / server_error / unexpected
 */
abstract class BaseDownstreamMetrics(
    private val registry: MeterRegistry,
    private val systemName: String,
) {

    /**
     * Timer for tidsbruk per operasjon mot et nedstrøms system.
     * Alle timere deler navnet `downstream_tid` og taggene (system, operation).
     */
    fun timer(operation: String): Timer =
        Timer.builder("downstream_tid")
            .description("Tidsbruk for kall mot nedstrøms systemer")
            .tags(
                "system", systemName,
                "operation", operation,
            )
            .publishPercentileHistogram()
            .register(registry)

    /**
     * Counter for kall per operasjon og resultat (success/timeout/feil).
     * Alle counters deler navnet `downstream_kall_total` og taggene (system, operation, result).
     */
    fun counter(operation: String, result: DownstreamResult): Counter =
        Counter.builder("downstream_kall_total")
            .description("Antall kall mot nedstrøms systemer")
            .tags(
                "system", systemName,
                "operation", operation,
                "result", result.name.lowercase(), // f.eks. success, timeout
            )
            .register(registry)
}
