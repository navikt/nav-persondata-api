package no.nav.persondataapi.konfigurasjon

import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientRequestObservationContext
import org.springframework.web.reactive.function.client.ClientRequestObservationConvention
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary


@Configuration
class ClientMetricsConfig {

    @Bean
    @Primary
    fun defaultClientObservationConvention(): ClientRequestObservationConvention =
        DefaultClientRequestObservationConvention()


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
    @Qualifier("aaregObservation")
    fun aaregObservationConvention(): ClientRequestObservationConvention =
        object : DefaultClientRequestObservationConvention() {
            override fun getLowCardinalityKeyValues(ctx: ClientRequestObservationContext): KeyValues {
                return super.getLowCardinalityKeyValues(ctx)
                    .and(KeyValue.of("system", "aareg"))
                    .and(KeyValue.of("operation", "arbeidsforhold"))
            }
        }

    @Bean
    @Qualifier("eeregObservation")
    fun eeregObservationConvention(): ClientRequestObservationConvention =
        object : DefaultClientRequestObservationConvention() {
            override fun getLowCardinalityKeyValues(ctx: ClientRequestObservationContext): KeyValues {
                return super.getLowCardinalityKeyValues(ctx)
                    .and(KeyValue.of("system", "eereg"))
                    .and(KeyValue.of("operation", "tester"))
            }
        }

    @Bean
    @Qualifier("utbetalingObservation")
    fun utbetalingObservationConvention(): ClientRequestObservationConvention =
        object : DefaultClientRequestObservationConvention() {
            override fun getLowCardinalityKeyValues(ctx: ClientRequestObservationContext): KeyValues {
                return super.getLowCardinalityKeyValues(ctx)
                    .and(KeyValue.of("system", "sokos-utbetaldata"))
                    .and(KeyValue.of("operation", "hent-utbetalingsinformasjon"))
            }
        }
    @Bean
    @Qualifier("pdlObservation")
    fun pdlObservationConvention(): ClientRequestObservationConvention =
        object : DefaultClientRequestObservationConvention() {
            override fun getLowCardinalityKeyValues(ctx: ClientRequestObservationContext): KeyValues {
                return super.getLowCardinalityKeyValues(ctx)
                    .and(KeyValue.of("system", "pdl"))
                    .and(KeyValue.of("operation", "hent-Person"))
            }
        }

    @Bean
    @Qualifier("kodeverkObservation")
    fun kodeverkObservationConvention(): ClientRequestObservationConvention =
        object : DefaultClientRequestObservationConvention() {
            override fun getLowCardinalityKeyValues(ctx: ClientRequestObservationContext): KeyValues {
                return super.getLowCardinalityKeyValues(ctx)
                    .and(KeyValue.of("system", "kodeverk"))
                    .and(KeyValue.of("operation", "hent-kodeverk"))
            }
        }

    @Bean
    @Qualifier("modiaContextHolderObservation")
    fun modiaContextHolderObservationConvention(): ClientRequestObservationConvention =
        object : DefaultClientRequestObservationConvention() {
            override fun getLowCardinalityKeyValues(ctx: ClientRequestObservationContext): KeyValues {
                return super.getLowCardinalityKeyValues(ctx)
                    .and(KeyValue.of("system", "modiacontextholder"))
                    .and(KeyValue.of("operation", "context"))
            }
        }

    @Bean
    @Qualifier("norg2Observation")
    fun norg2ObservationConvention(): ClientRequestObservationConvention =
        object : DefaultClientRequestObservationConvention() {
            override fun getLowCardinalityKeyValues(ctx: ClientRequestObservationContext): KeyValues {
                return super.getLowCardinalityKeyValues(ctx)
                    .and(KeyValue.of("system", "norg2"))
                    .and(KeyValue.of("operation", "navkontor"))
            }
        }

}
