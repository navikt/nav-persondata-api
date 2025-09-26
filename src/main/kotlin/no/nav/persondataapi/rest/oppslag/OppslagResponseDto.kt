package no.nav.persondataapi.rest.oppslag

data class OppslagResponseDto<T>(
    val error: String? = null,
    val data: T? = null,
)
