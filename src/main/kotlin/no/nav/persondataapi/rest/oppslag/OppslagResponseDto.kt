package no.nav.persondataapi.rest.oppslag

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Standardrespons for oppslag av persondata")
data class OppslagResponseDto<T>(
    @field:Schema(
        description = "Feilmelding hvis oppslaget feilet",
        example = "Ingen tilgang",
        nullable = true
    )
    val error: String? = null,

    @field:Schema(
        description = "Resultatdata fra oppslaget",
        nullable = true
    )
    val data: T? = null,
)
