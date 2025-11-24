package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.norg2.client.NavLokalKontor
import no.nav.persondataapi.integrasjon.norg2.client.Norg2Client
import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.konfigurasjon.teamLogsMarker
import no.nav.persondataapi.responstracing.erTraceLoggingAktvert
import no.nav.persondataapi.rest.domene.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NavTilhørighetService(
    private val pdlClient: PdlClient,
    private val norg2Client: Norg2Client
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun finnLokalKontorForPersonIdent(personIdent: PersonIdent): NavLokalKontor {
        val geografiskTilknytning = pdlClient.hentGeografiskTilknytning(personIdent)
        if (erTraceLoggingAktvert()) {
            logger.info(teamLogsMarker,"Logging aktivert - full PDL-geografisk-Tilknytning respons for {}: {}", personIdent, JsonUtils.toJson(geografiskTilknytning).toPrettyString())
        }
        if (geografiskTilknytning.data == null || geografiskTilknytning.data.gtKommune == null) {
            return NavLokalKontor(
                -1,
                "Ukjent",
                "",
                ""
            )
        }
        return norg2Client.hentLokalNavKontor(geografiskTilknytning.data.gtKommune)
    }
}