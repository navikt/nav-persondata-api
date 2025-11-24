package no.nav.persondataapi.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class PdlMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "pdl")

@Component
class UtbetalingMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "utbetaling")

@Component
class KontoregisterMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "kontoregister")

@Component
class InntektMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "inntekt")

@Component
class AaregMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "aareg")

@Component
class EregMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "ereg")

@Component
class Norg2Metrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "norg2")
