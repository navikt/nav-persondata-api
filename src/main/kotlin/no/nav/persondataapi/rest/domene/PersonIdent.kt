package no.nav.persondataapi.rest.domene

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Fødselsnummer eller D-nummer for en person",
    example = "12345678901",
    type = "string"
)
@JvmInline
value class PersonIdent @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @get:JsonValue val value: String
) {
    override fun toString(): String = value.replaceRange(6, 11, "*****")
}
