package no.nav.persondataapi.tracelogging
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.konfigurasjon.teamLogsMarker
import no.nav.persondataapi.responstracing.erTraceLoggingAktvert
import no.nav.persondataapi.rest.domene.PersonIdent
import org.slf4j.Logger


fun traceLoggHvisAktivert(logger: Logger, kilde:String, personIdent: PersonIdent, unit: Any){
    if (erTraceLoggingAktvert()) {
        logger.info(teamLogsMarker,"Trace-logging aktivert for{} - $kilde", personIdent,
        kv("kilde",kilde),
        kv("personIdent",personIdent.value),
        kv("json", JsonUtils.toJson(unit).toPrettyString())
        )
    }
}
