package no.nav.persondataapi.service

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.persondataapi.generated.pdl.enums.GtType
import no.nav.persondataapi.generated.pdl.hentgeografisktilknytning.GeografiskTilknytning
import no.nav.persondataapi.integrasjon.norg2.client.NavLokalKontor
import no.nav.persondataapi.integrasjon.norg2.client.Norg2Client
import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.konfigurasjon.teamLogsMarker
import no.nav.persondataapi.responstracing.erTraceLoggingAktvert
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.tracelogging.traceLogg
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NavTilhørighetService(
    private val pdlClient: PdlClient, private val norg2Client: Norg2Client
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun finnLokalKontorForPersonIdent(personIdent: PersonIdent): NavLokalKontor {
        val geografiskTilknytning = pdlClient.hentGeografiskTilknytning(personIdent)
        if (erTraceLoggingAktvert()) {
            traceLogg(
                logger = logger,
                kilde = "PDL geografisk-tilknytning",
                personIdent=personIdent,
                unit = geografiskTilknytning
            )
        }
        val norgIdent = geografiskTilknytning.data?.hentNorgIdent()
        if (norgIdent == null) {
            return NavLokalKontor(
                -1, "Uklart", "", ""
            )
        }
        return norg2Client.hentLokalNavKontor(norgIdent)

    }
}

fun GeografiskTilknytning.hentNorgIdent(): String? {
    return when (this.gtType) {
        GtType.BYDEL -> this.gtBydel
        GtType.KOMMUNE -> this.gtKommune
        GtType.UTLAND -> null
        GtType.UDEFINERT -> null
        GtType.__UNKNOWN_VALUE -> null
    }
}
