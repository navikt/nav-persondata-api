package no.nav.persondataapi.konfigurasjon

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfiguration {

    @Bean
    fun cacheManager(cacheProperties: CacheProperties): CacheManager {
        val cacheManager = CaffeineCacheManager()
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(cacheProperties.defaultExpiration)
                .maximumSize(cacheProperties.maximumSize)
        )

        // Registrer alle custom caches med deres spesifikke expiration
        cacheProperties.caches.forEach { (cacheName, config) ->
            cacheManager.registerCustomCache(
                cacheName,
                Caffeine.newBuilder()
                    .expireAfterWrite(config.expiration)
                    .maximumSize(config.maximumSize ?: cacheProperties.maximumSize)
                    .build()
            )
        }

        return cacheManager
    }

    @Bean
    @ConfigurationProperties(prefix = "cache")
    fun cacheProperties(): CacheProperties {
        return CacheProperties()
    }
}

data class CacheProperties(
    var defaultExpiration: Duration = Duration.ofHours(1),
    var maximumSize: Long = 1000,
    var caches: Map<String, CacheConfig> = emptyMap()
)

data class CacheConfig(
    var expiration: Duration = Duration.ofHours(1),
    var maximumSize: Long? = null
)
