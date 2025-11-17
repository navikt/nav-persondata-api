package no.nav.persondataapi.service

import no.nav.persondataapi.rest.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class CacheAdminServiceTest {
	private lateinit var cacheManager: ConcurrentMapCacheManager
	private lateinit var service: CacheAdminService

	@BeforeEach
	fun setup() {
		cacheManager =
			ConcurrentMapCacheManager(
				"pdl-person",
				"aareg-arbeidsforhold",
				"utbetaling-bruker",
				"inntekt-historikk",
				"kodeverk-landkoder",
			)
		service = CacheAdminService(cacheManager)
	}

	@Test
	fun `flushAlleCacher tømmer alle cacher`() {
		cacheManager.getCache("pdl-person")!!.put(PersonIdent("12345678910"), "verdi")
		cacheManager.getCache("kodeverk-landkoder")!!.put("NO", "Norge")

		val summary = service.flushAlleCacher()

		cacheManager.cacheNames.forEach { cacheName ->
			val cache = cacheManager.getCache(cacheName)!!
			assertTrue(
				cache.nativeCacheAsMap().isEmpty(),
				"Cache $cacheName skal være tom",
			)
		}
		assertEquals(
			cacheManager.cacheNames.map { it }.sorted(),
			summary.flushedeCacher,
		)
		assertEquals(CacheFlushScope.ALLE, summary.scope)
		assertNull(summary.personIdent)
	}

	@Test
	fun `flushCacherForPersonIdent fjerner person cacher for en gitt person, og clearer inntektscache`() {
		val ident = PersonIdent("12345678910")
		cacheManager.getCache("pdl-person")!!.put(ident, "pdl")
		cacheManager.getCache("aareg-arbeidsforhold")!!.put(ident, "aareg")
		cacheManager.getCache("utbetaling-bruker")!!.put(ident, "utbetaling")
		cacheManager.getCache("inntekt-historikk")!!.put("12345678910_2020-01-01_2020-12-31", "inntekt")
		cacheManager.getCache("kodeverk-landkoder")!!.put("NO", "Norge")

		val oppsummering = service.flushCacherForPersonIdent(ident)

		listOf("pdl-person", "aareg-arbeidsforhold", "utbetaling-bruker").forEach { cacheName ->
			val cache = cacheManager.getCache(cacheName)!!
			assertNull(cache.nativeCacheAsMap()[ident], "Cache $cacheName skal være tømt for ident")
		}
		assertTrue(
			cacheManager.getCache("inntekt-historikk")!!.nativeCacheAsMap().isEmpty(),
			"inntekt-historikk skal være tømt",
		)
		// øvrige cache skal fortsatt ha verdi
		val kodeverkCache = cacheManager.getCache("kodeverk-landkoder")!!
		assertEquals("Norge", kodeverkCache.nativeCacheAsMap()["NO"])

		assertEquals(CacheFlushScope.PERSON, oppsummering.scope)
		assertEquals("123456*****", oppsummering.personIdent)
		assertEquals(listOf("aareg-arbeidsforhold", "inntekt-historikk", "pdl-person", "utbetaling-bruker"), oppsummering.flushedeCacher)
	}

	@Suppress("UNCHECKED_CAST")
	private fun org.springframework.cache.Cache.nativeCacheAsMap(): MutableMap<Any, Any> = this.nativeCache as MutableMap<Any, Any>
}
