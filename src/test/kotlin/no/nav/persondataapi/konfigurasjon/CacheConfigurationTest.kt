package no.nav.persondataapi.konfigurasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.persondataapi.integrasjon.pdl.client.PersonDataResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheConfigurationTest {
	private val objectMapper =
		ObjectMapper()
			.registerModule(KotlinModule.Builder().build())
			.registerModule(JavaTimeModule())

	@Test
	fun `redis serializer round-trips PersonDataResultat`() {
		val serializer = CacheConfiguration.createRedisSerializer(objectMapper)
		val original =
			PersonDataResultat(
				data = null,
				statusCode = 200,
				errorMessage = "feilmelding",
			)

		val bytes = serializer.serialize(original)
		val restored = serializer.deserialize(bytes)

		assertTrue(restored is PersonDataResultat, "Deserialisering skal gi PersonDataResultat")
		assertEquals(original, restored)
	}
}
