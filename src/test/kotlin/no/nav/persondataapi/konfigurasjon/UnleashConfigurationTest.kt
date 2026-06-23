package no.nav.persondataapi.konfigurasjon

import io.getunleash.FakeUnleash
import no.nav.persondataapi.tokenutilities.NAV_IDENT
import no.nav.persondataapi.unleash.UnleashFeatureToggleService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class UnleashConfigurationTest {
    private val config = UnleashConfiguration()

    @Test
    fun `unleash() returnerer FakeUnleash når URL er blank`() {
        val unleash = config.unleash(apiUrl = "", apiToken = "token")
        assertInstanceOf(FakeUnleash::class.java, unleash)
    }

    @Test
    fun `unleash() returnerer FakeUnleash når token er blank`() {
        val unleash = config.unleash(apiUrl = "https://unleash.example.com", apiToken = "")
        assertInstanceOf(FakeUnleash::class.java, unleash)
    }

    @Test
    fun `unleash() med FakeUnleash har alle toggles aktivert`() {
        val unleash = config.unleash(apiUrl = "", apiToken = "") as FakeUnleash
        assertTrue(unleash.isEnabled("hvilken-som-helst-toggle"))
    }

    @Test
    fun `unleash() returnerer DefaultUnleash når URL og token er satt`() {
        val unleash = config.unleash(apiUrl = "https://unleash.example.com", apiToken = "token")
        try {
            assertFalse(unleash is FakeUnleash)
        } finally {
            unleash.shutdown()
        }
    }

    @Test
    fun `featureToggleService() returnerer UnleashFeatureToggleService`() {
        val unleash = FakeUnleash()
        val service = config.featureToggleService(unleash)
        assertInstanceOf(UnleashFeatureToggleService::class.java, service)
    }
}

class NavIdentUnleashContextProviderTest {
    private val provider = NavIdentUnleashContextProvider()

    @Test
    fun `getContext() bruker NAVident fra MDC`() {
        MDC.put(NAV_IDENT, "Z123456")
        try {
            val context = provider.getContext()
            assertEquals("Z123456", context.userId.orElse(null))
        } finally {
            MDC.remove(NAV_IDENT)
        }
    }

    @Test
    fun `getContext() returnerer null userId når MDC ikke er satt`() {
        MDC.remove(NAV_IDENT)
        val context = provider.getContext()
        assertNull(context.userId.orElse(null))
    }
}
