package no.nav.persondataapi.configuration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

object JsonUtils {
    val mapper = jacksonObjectMapper().apply {
        findAndRegisterModules() // registrerer kotlin, JavaTime, etc.
    }

    inline fun <reified T> fromJson(json: String): T = mapper.readValue(json)
}