package no.nav.persondataapi.rest.oppslag

import no.nav.persondataapi.rest.domain.PersonIdent

data class OppslagRequestDto(
    val ident: PersonIdent
)
