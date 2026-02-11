package no.nav.persondataapi.rest.oppslag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.persondataapi.rest.domene.PersonIdent

@Schema(description = "Forespørsel for oppslag av persondata")
data class OppslagRequestDto(
    @field:Schema(
        description = "Fødselsnummer eller D-nummer for personen det skal gjøres oppslag på",
        example = "12345678901",
        required = true
    )
    val ident: PersonIdent
)
