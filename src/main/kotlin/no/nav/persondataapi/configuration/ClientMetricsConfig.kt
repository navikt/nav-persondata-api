package no.nav.persondataapi.configuration

import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues
import io.micrometer.core.instrument.MeterRegistry

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientRequestObservationContext
import org.springframework.web.reactive.function.client.ClientRequestObservationConvention
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer


@Configuration
class ClientMetricsConfig {

    @Bean
    @Qualifier("inntektObservation")
    fun inntektObservationConvention(): ClientRequestObservationConvention =
        object : DefaultClientRequestObservationConvention() {
            override fun getLowCardinalityKeyValues(ctx: ClientRequestObservationContext): KeyValues {
                return super.getLowCardinalityKeyValues(ctx)
                    .and(KeyValue.of("system", "inntekt"))
                    .and(KeyValue.of("operation", "hentInntekter"))
            }
        }

    @Bean
    fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> =
        MeterRegistryCustomizer { registry ->
            registry.config().commonTags(
                "app", "persondataapi",
                "team", "kontroll"
            )
        }
}
