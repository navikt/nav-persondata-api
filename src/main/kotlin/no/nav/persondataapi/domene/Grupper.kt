package no.nav.persondataapi.domene

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

data class RolleConfig(
	val id: String,
	val name: String,
)

@Component
class Grupper(
	@param:Value("\${GRUPPER}") private val rollerJson: String,
) {
	private val mapper = jacksonObjectMapper()
	val roller: List<RolleConfig> = mapper.readValue(rollerJson)

	fun finnRolleId(navn: String): String? = roller.find { it.name == navn }?.id
}
