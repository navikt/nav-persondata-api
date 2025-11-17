package no.nav.persondataapi.rest.oppslag

import no.nav.persondataapi.rest.domene.PersonIdent

data class OppslagRequestDto(
	val ident: PersonIdent,
)
