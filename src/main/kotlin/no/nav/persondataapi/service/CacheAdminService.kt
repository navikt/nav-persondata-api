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

    private val personIdentCacher = setOf(
        "pdl-person",
        "pdl-geografisktilknytning",
        "aareg-arbeidsforhold",
        "utbetaling-bruker",
        "norg2-lokalKontor",
    )

    private val cacherSomTømmesPåPersonFlush = setOf(
        "inntekt-historikk",
        "pensjonsgivende-inntekt",
    )

    fun flushAlleCacher(): CacheFlushOppsummering {
        val tømteCacher = cacheManager.cacheNames
            .mapNotNull { name ->
                cacheManager.getCache(name)
                    ?.also { it.clear() }
                    ?.let { name }
            }
            .sorted()

        logger.info("Flushet alle cacher: {}", tømteCacher)

        return CacheFlushOppsummering(
            flushedeCacher = tømteCacher,
            scope = CacheFlushScope.ALLE
        )
    }

    fun flushCacherForPersonIdent(personIdent: PersonIdent): CacheFlushOppsummering {
        val flushede = mutableSetOf<String>()

        personIdentCacher.forEach { cacheName ->
            cacheManager.fjernFraCache(cacheName, personIdent)?.let { flushede.add(cacheName) }
        }

        cacherSomTømmesPåPersonFlush.forEach { cacheName ->
            cacheManager.tøm(cacheName)?.let { flushede.add(cacheName) }
        }

        val oppsummering = CacheFlushOppsummering(
            flushedeCacher = flushede.toList().sorted(),
            scope = CacheFlushScope.PERSON,
            personIdent = personIdent.toString()
        )

        logger.info("Flushet cacher for {}: {}", personIdent, oppsummering.flushedeCacher)

        return oppsummering
    }

    private fun CacheManager.fjernFraCache(cacheName: String, key: Any): Cache? =
        getCache(cacheName)?.also { cache ->
            cache.evict(key)
        }

    private fun CacheManager.tøm(cacheName: String): Cache? =
        getCache(cacheName)?.also(Cache::clear)
}

data class CacheFlushOppsummering(
    val flushedeCacher: List<String>,
    val scope: CacheFlushScope,
    val personIdent: String? = null,
)

enum class CacheFlushScope {
    ALLE,
    PERSON,
}
