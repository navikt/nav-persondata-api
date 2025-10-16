package no.nav.persondataapi.rest.domene

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class PersonIdent @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @get:JsonValue val value: String
) {
    override fun toString(): String = value.replaceRange(6, 11, "*****")
}
