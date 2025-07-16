package no.nav.persondataapi.service.dataproviders


import no.nav.persondataapi.pdl.PdlClient
import no.nav.persondataapi.service.dataproviders.GrunnlagsProvider
import no.nav.persondataapi.service.dataproviders.GrunnlagsType
import no.nav.persondataapi.service.dataproviders.GrunnlagsdelResultat
import no.nav.persondataapi.utbetaling.client.UtbetalingClient

import org.springframework.stereotype.Component

@Component
class PersonDataGrunnlagsProvider(val pdlClient: PdlClient) : GrunnlagsProvider {
    override val type = GrunnlagsType.PERSONDATA

    override suspend fun hent(kontekst: GrunnlagsKontekst): GrunnlagsdelResultat {
        val resultat = pdlClient.hentPersonv2(kontekst.fnr,kontekst.token)

        return GrunnlagsdelResultat(
            type = type,
            data = resultat.data,
            ok = resultat.statusCode in 200..299,
            status = resultat.statusCode,
            feilmelding = resultat.errorMessage
        )
    }
}
