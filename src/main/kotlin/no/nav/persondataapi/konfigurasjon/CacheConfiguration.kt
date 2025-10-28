package no.nav.persondataapi.konfigurasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfiguration {

    private val logger = LoggerFactory.getLogger(CacheConfiguration::class.java)

    @Bean
    @Profile("!local")
    fun redisCacheManager(
        cacheProperties: CacheProperties,
        redisConnectionFactory: RedisConnectionFactory,
    ): CacheManager {
        logger.info("Konfigurerer cache manager med Valkey/Redis backend")

        val defaultConfig = redisCacheConfiguration(cacheProperties.defaultExpiration)
        val cacheConfigurations = cacheProperties.caches.mapValues { (_, config) ->
            redisCacheConfiguration(config.expiration)
        }

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    @Bean
    @Profile("local")
    fun caffeineCacheManager(cacheProperties: CacheProperties): CacheManager {
        logger.info("Konfigurerer cache manager med in-memory Caffeine backend")

        val cacheManager = CaffeineCacheManager()
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(cacheProperties.defaultExpiration)
                .maximumSize(cacheProperties.maximumSize)
        )

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

  private fun redisCacheConfiguration(ttl: Duration): RedisCacheConfiguration {
    val serializer = GenericJackson2JsonRedisSerializer() // uses built-in mapper with type info
    return RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(ttl)
      .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
      .disableCachingNullValues()
  }

    @Bean
    @ConfigurationProperties(prefix = "cache")
    fun cacheProperties(): CacheProperties = CacheProperties()
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
