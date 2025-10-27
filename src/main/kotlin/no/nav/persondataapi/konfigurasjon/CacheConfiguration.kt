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
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfiguration {

    private val logger = LoggerFactory.getLogger(CacheConfiguration::class.java)

    @Bean
    fun cacheManager(
        cacheProperties: CacheProperties,
        valkeyProperties: ValkeyProperties,
        objectMapper: ObjectMapper
    ): CacheManager {
        val resolvedValkeyProps = finnValkeyProperties(valkeyProperties)
        return if (resolvedValkeyProps.useValkey()) {
            logger.info("Konfigurerer cache manager med Valkey backend")
            opprettRemoteCacheManager(cacheProperties, resolvedValkeyProps, objectMapper)
        } else {
            logger.info("Konfigurerer cache manager med in-memory Caffeine backend")
            opprettInMemoryCacheManager(cacheProperties)
        }
    }

    private fun opprettInMemoryCacheManager(cacheProperties: CacheProperties): CacheManager {
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

    private fun opprettRemoteCacheManager(
        cacheProperties: CacheProperties,
        valkeyProperties: ValkeyProperties,
        objectMapper: ObjectMapper
    ): CacheManager {
        val connectionFactory = opprettLettuceConnectionFactory(valkeyProperties)
        connectionFactory.afterPropertiesSet()

        val defaultConfig = redisCacheConfiguration(cacheProperties.defaultExpiration, objectMapper)
        val cacheConfigurations = cacheProperties.caches.mapValues { (_, config) ->
            redisCacheConfiguration(config.expiration, objectMapper)
        }

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    private fun finnValkeyProperties(valkeyProperties: ValkeyProperties): ValkeyProperties {
        val environment = System.getenv()

        if (valkeyProperties.host.isNullOrBlank()) {
            valkeyProperties.host = environment.entries.firstOrNull {  it.key.startsWith("VALKEY_HOST_") }?.value
        }

        if (valkeyProperties.password.isNullOrBlank()) {
            valkeyProperties.password = environment.entries.firstOrNull {  it.key.startsWith("VALKEY_PASSWORD_") }?.value
        }

        valkeyProperties.port = environment.entries.firstOrNull {  it.key.startsWith("VALKEY_PORT_") }?.value?.toIntOrNull() ?: valkeyProperties.port

        return valkeyProperties
    }

    private fun opprettLettuceConnectionFactory(valkeyProperties: ValkeyProperties): LettuceConnectionFactory {
        val host = valkeyProperties.requireHost()
        val standaloneConfig = RedisStandaloneConfiguration(host, valkeyProperties.port)

        if (!valkeyProperties.password.isNullOrBlank()) {
            standaloneConfig.password = RedisPassword.of(valkeyProperties.password)
        }

        val clientConfigBuilder = LettuceClientConfiguration.builder()
            .commandTimeout(valkeyProperties.commandTimeout)

        if (valkeyProperties.sslEnabled) {
            clientConfigBuilder.useSsl()
        }

        return LettuceConnectionFactory(standaloneConfig, clientConfigBuilder.build())
    }

    private fun redisCacheConfiguration(ttl: Duration, objectMapper: ObjectMapper): RedisCacheConfiguration {
        val serializer = GenericJackson2JsonRedisSerializer(objectMapper)
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )
            .disableCachingNullValues()
    }

    @Bean
    @ConfigurationProperties(prefix = "cache")
    fun cacheProperties(): CacheProperties = CacheProperties()

    @Bean
    @ConfigurationProperties(prefix = "valkey")
    fun valkeyProperties(): ValkeyProperties = ValkeyProperties()
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

data class ValkeyProperties(
    var enabled: Boolean? = null,
    var host: String? = null,
    var port: Int = 6379,
    var password: String? = null,
    var sslEnabled: Boolean = true,
    var commandTimeout: Duration = Duration.ofSeconds(3)
) {
    fun useValkey(): Boolean =
        when (enabled) {
            null -> !host.isNullOrBlank()
            else -> enabled!!
        }

    fun requireHost(): String =
        host?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("valkey.host må være satt når Valkey caching er slått på")
}
