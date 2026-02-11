package no.nav.persondataapi.rest.oppslag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.persondataapi.rest.domene.PersonIdent

@Schema(description = "Forespørsel for logging av begrunnet tilgang")
data class BegrunnelseRequestDto(
    @field:Schema(
        description = "Fødselsnummer eller D-nummer for personen",
        example = "12345678901",
        required = true
    )
    val ident: PersonIdent,

    @field:Schema(
        description = "Saksbehandlers begrunnelse for tilgangen",
        example = "Vurderer søknad om dagpenger",
        required = true
    )
    val begrunnelse: String,

    @field:Schema(
        description = "Type mangel eller årsak til begrunnet tilgang",
        example = "Mangler vedtak",
        required = true
    )
    val mangel: String
)
