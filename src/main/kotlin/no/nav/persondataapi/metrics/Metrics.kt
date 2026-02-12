package no.nav.persondataapi.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class PdlMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "pdl")

@Component
class UtbetalingMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "utbetaling")

@Component
class InntektMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "inntekt")

@Component
class AaregMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "aareg")

@Component
class EregMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "ereg")

@Component
class Norg2Metrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "norg2")

@Component
class DPDatadelingMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "dp-datadeling")

@Component
class AAPMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "aap")

@Component
class NomMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "nom")

@Component
class SigrunMetrics(registry: MeterRegistry) : BaseDownstreamMetrics(registry, "sigrun")
