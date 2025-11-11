package no.nav.persondataapi.rest.oppslag

import no.nav.persondataapi.rest.domene.PersonIdent

data class BegrunnelseRequestDto(
    val ident: PersonIdent,
    val begrunnelse: String
)
