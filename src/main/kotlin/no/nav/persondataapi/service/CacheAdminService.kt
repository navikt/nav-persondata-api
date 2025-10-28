package no.nav.persondataapi.service

import no.nav.persondataapi.rest.domene.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class CacheAdminService(
    private val cacheManager: CacheManager
) {

    private val logger = LoggerFactory.getLogger(CacheAdminService::class.java)

    private val personIdentCaches = setOf(
        "pdl-person",
        "aareg-arbeidsforhold",
        "utbetaling-bruker",
    )

    private val cachesClearedOnPersonFlush = setOf(
        "inntekt-historikk",
    )

    fun flushAllCaches(): CacheFlushSummary {
        val clearedCaches = cacheManager.cacheNames
            .mapNotNull { name ->
                cacheManager.getCache(name)
                    ?.also { it.clear() }
                    ?.let { name }
            }
            .sorted()

        logger.info("Flushed all caches: {}", clearedCaches)

        return CacheFlushSummary(
            flushedCaches = clearedCaches,
            scope = CacheFlushScope.ALL
        )
    }

    fun flushCachesForPerson(personIdent: PersonIdent): CacheFlushSummary {
        val flushed = mutableSetOf<String>()

        personIdentCaches.forEach { cacheName ->
            cacheManager.evict(cacheName, personIdent)?.let { flushed.add(cacheName) }
        }

        cachesClearedOnPersonFlush.forEach { cacheName ->
            cacheManager.clear(cacheName)?.let { flushed.add(cacheName) }
        }

        val summary = CacheFlushSummary(
            flushedCaches = flushed.toList().sorted(),
            scope = CacheFlushScope.PERSON,
            personIdent = personIdent.toString()
        )

        logger.info("Flushed caches for {}: {}", personIdent, summary.flushedCaches)

        return summary
    }

    private fun CacheManager.evict(cacheName: String, key: Any): Cache? =
        getCache(cacheName)?.also { cache ->
            cache.evict(key)
        }

    private fun CacheManager.clear(cacheName: String): Cache? =
        getCache(cacheName)?.also(Cache::clear)
}

data class CacheFlushSummary(
    val flushedCaches: List<String>,
    val scope: CacheFlushScope,
    val personIdent: String? = null,
)

enum class CacheFlushScope {
    ALL,
    PERSON,
}
