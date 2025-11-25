package no.nav.persondataapi.service

import no.nav.persondataapi.generated.enums.GtType
import no.nav.persondataapi.generated.hentgeografisktilknytning.GeografiskTilknytning
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
        val norgIdent = geografiskTilknytning.data?.hentNorgIdent()
        if (norgIdent != null) {
            return norg2Client.hentLokalNavKontor(norgIdent)
        }
        else  {
            return NavLokalKontor(
                -1,
                "Ukjent",
                "",
                ""
            )
        }
    }
}

fun GeografiskTilknytning.hentNorgIdent():String?{
    when (this.gtType){
      GtType.BYDEL -> return  this.gtBydel
        GtType.KOMMUNE -> return  this.gtKommune
        GtType.UTLAND -> return  this.gtLand
        GtType.UDEFINERT -> return null
        GtType.__UNKNOWN_VALUE -> return null
    }
}