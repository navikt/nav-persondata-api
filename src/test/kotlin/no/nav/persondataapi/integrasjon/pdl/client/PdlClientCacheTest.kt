package no.nav.persondataapi.integrasjon.pdl.client

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.persondataapi.konfigurasjon.CacheConfiguration
import no.nav.persondataapi.konfigurasjon.CacheConfig
import no.nav.persondataapi.konfigurasjon.CacheProperties
import no.nav.persondataapi.konfigurasjon.ValkeyProperties
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.time.Duration

class PdlClientCacheTest {

    @Test
    fun `cache skal være konfigurert med riktige innstillinger`() {
        val cacheProperties = CacheProperties(
            defaultExpiration = Duration.ofHours(1),
            maximumSize = 1000,
            caches = mapOf(
                "pdl-person" to CacheConfig(
                    expiration = Duration.ofHours(1),
                    maximumSize = 1000
                )
            )
        )

        val cacheConfiguration = CacheConfiguration()
        val cacheManager: CacheManager = cacheConfiguration.cacheManager(
            cacheProperties = cacheProperties,
            valkeyProperties = ValkeyProperties(enabled = false),
            objectMapper = ObjectMapper().findAndRegisterModules()
        )

        assertNotNull(cacheManager, "CacheManager skal være konfigurert")

        // Verifiser at pdl-person cache er registrert
        val cache = cacheManager.getCache("pdl-person")
        assertNotNull(cache, "pdl-person cache skal eksistere")
    }

    @Test
    fun `valkey blir aktivert når host er satt`() {
        val properties = ValkeyProperties(host = "valkey.dev", enabled = null)
        assertTrue(properties.useValkey(), "Valkey skal aktiveres når host er definert")
    }

    @Test
    fun `valkey kan eksplisitt deaktiveres`() {
        val properties = ValkeyProperties(host = "valkey.dev", enabled = false)
        assertFalse(properties.useValkey(), "Valkey skal kunne deaktiveres eksplisitt")
    }
}
