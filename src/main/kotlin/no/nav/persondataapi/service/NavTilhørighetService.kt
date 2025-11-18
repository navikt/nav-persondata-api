package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.norg2.client.NavLokalKontor
import no.nav.persondataapi.integrasjon.norg2.client.Norg2Client
import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.rest.domene.PersonIdent
import org.springframework.stereotype.Service

@Service
class NavTilhørighetService(
    private val pdlClient: PdlClient,
    private val norg2Client: Norg2Client
) {

    suspend fun finnLokalKontorForPersonIdent(personIdent: PersonIdent): NavLokalKontor {
        val geografiskTilknytning = pdlClient.hentGeografiskTilknytning(personIdent)
        if (geografiskTilknytning.data == null) {
            return NavLokalKontor(
                -1,
                "",
                "",
                ""
            )
        }
        return norg2Client.hentLokalNavKontor(geografiskTilknytning.data?.gtKommune!!)
    }
}