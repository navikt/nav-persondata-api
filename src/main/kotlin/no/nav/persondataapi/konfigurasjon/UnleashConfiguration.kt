package no.nav.persondataapi.konfigurasjon

import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.util.UnleashConfig
import no.nav.persondataapi.tokenutilities.NAV_IDENT
import no.nav.persondataapi.unleash.FeatureToggleService
import no.nav.persondataapi.unleash.UnleashFeatureToggleService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UnleashConfiguration {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun unleash(): Unleash {
        val apiUrl = System.getenv("UNLEASH_SERVER_API_URL")
        val apiToken = System.getenv("UNLEASH_SERVER_API_TOKEN")

        if (apiUrl.isNullOrBlank() || apiToken.isNullOrBlank()) {
            log.info("UNLEASH_SERVER_API_URL ikke satt — bruker FakeUnleash (alle toggles false)")
            return FakeUnleash()
        }

        val config =
            UnleashConfig
                .builder()
                .appName("nav-persondata-api")
                .instanceId("nav-persondata-api")
                .unleashAPI("$apiUrl/api")
                .apiKey(apiToken)
                .unleashContextProvider(NavIdentUnleashContextProvider())
                .synchronousFetchOnInitialisation(true)
                .build()

        return DefaultUnleash(config)
    }

    @Bean
    fun featureToggleService(unleash: Unleash): FeatureToggleService = UnleashFeatureToggleService(unleash)
}

private class NavIdentUnleashContextProvider : UnleashContextProvider {
    override fun getContext(): UnleashContext =
        UnleashContext
            .builder()
            .userId(MDC.get(NAV_IDENT))
            .build()
}
