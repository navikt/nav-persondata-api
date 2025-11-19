package no.nav.persondataapi.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.persondataapi.konfigurasjon.teamLogsMarker
import no.nav.persondataapi.rest.domene.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BegrunnetTilgangService(private val registry: MeterRegistry
) {
    private val begrunnerTilgang = Counter.builder("BegrunnetTilgang.teller")
        .description("Antall ganger begrunnet tilgang er gitt")
        .register(registry)


    private val logger = LoggerFactory.getLogger(javaClass)

    fun loggBegrunnetTilgang(personIdent: PersonIdent, begrunnelse: String, mangel: String) {
        logger.info("Begrunnet tilgang registrert")
        begrunnerTilgang.increment()
        logger.info(
            teamLogsMarker,
            "Begrunnet tilgang registrert; ident={} melding={} mangel={}",
            personIdent.value,
            begrunnelse,
            mangel,
        )
    }
}
