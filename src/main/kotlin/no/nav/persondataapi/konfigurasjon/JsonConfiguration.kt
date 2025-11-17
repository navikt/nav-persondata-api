package no.nav.persondataapi.konfigurasjon

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.math.BigDecimal

object JsonUtils {
	val mapper: ObjectMapper =
		ObjectMapper()
			.registerModule(KotlinModule.Builder().build())
			.registerModule(JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.registerModule(
				SimpleModule().addSerializer(BigDecimal::class.java, ToStringSerializer.instance),
			)

	inline fun <reified T> fromJson(json: String): T = mapper.readValue(json)

	fun toJson(obj: Any): JsonNode = mapper.valueToTree(obj)
}
