package no.nav.persondataapi.rest.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class PersonIdent @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @get:JsonValue val value: String
) {
    init {
        require(value.matches(Regex("\\d{11}"))) { "PersonIdent må være nøyaktig 11 tall" }
    }

    override fun toString(): String = value.replaceRange(6, 11, "*****")
}
