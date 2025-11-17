package no.nav.persondataapi.responstracing

import org.slf4j.MDC
import kotlin.text.toBoolean

const val LOGG_HEADER = "logg"

fun erTraceLoggingAktvert(): Boolean = MDC.get(LOGG_HEADER)?.toBoolean() ?: false
