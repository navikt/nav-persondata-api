package no.nav.persondataapi.konfigurasjon

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.ToStringSerializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal


object JsonUtils {
    val mapper: JsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .addModule(
            SimpleModule().addSerializer(BigDecimal::class.java, ToStringSerializer.instance)
        )
        .build()

    inline fun <reified T> fromJson(json: String): T =
        mapper.readValue(json)

    fun toJson(obj: Any): JsonNode =
        mapper.valueToTree(obj)
}
