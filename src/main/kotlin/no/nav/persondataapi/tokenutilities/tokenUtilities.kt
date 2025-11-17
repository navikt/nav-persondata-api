package no.nav.persondataapi.tokenutilities

import org.slf4j.MDC


const val NAV_IDENT = "navIdent"


fun hentNavIdent():String {
    return MDC.get(NAV_IDENT) ?: "ukjent"
}