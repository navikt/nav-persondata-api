package no.nav.persondataapi.integrasjon.pdl.client

import no.nav.persondataapi.konfigurasjon.CacheConfiguration
import no.nav.persondataapi.konfigurasjon.CacheProperties
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import kotlin.test.assertNotNull
import java.time.Duration

class PdlClientCacheTest {

    @Test
    fun `cache skal være konfigurert med riktige innstillinger`() {
        val cacheProperties = CacheProperties(
            defaultExpiration = Duration.ofHours(1),
            maximumSize = 1000,
            caches = mapOf(
                "pdl-person" to no.nav.persondataapi.konfigurasjon.CacheConfig(
                    expiration = Duration.ofHours(1),
                    maximumSize = 1000
                )
            )
        )

        val cacheConfiguration = CacheConfiguration()
        val cacheManager: CacheManager = cacheConfiguration.cacheManager(cacheProperties)

        assertNotNull(cacheManager, "CacheManager skal være konfigurert")

        // Verifiser at pdl-person cache er registrert
        val cache = cacheManager.getCache("pdl-person")
        assertNotNull(cache, "pdl-person cache skal eksistere")
    }
}
