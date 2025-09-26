package no.nav.persondataapi.service.dataproviders


import no.nav.persondataapi.aareg.client.AaregClient
import no.nav.persondataapi.service.dataproviders.GrunnlagsProvider
import no.nav.persondataapi.service.dataproviders.GrunnlagsType
import no.nav.persondataapi.service.dataproviders.GrunnlagsdelResultat
import no.nav.persondataapi.utbetaling.client.UtbetalingClient

import org.springframework.stereotype.Component

@Component
class AAregGrunnlagsProvider(val aaregClient: AaregClient) : GrunnlagsProvider {
    override val type = GrunnlagsType.ARBEIDSFORHOLD

    override suspend fun hent(kontekst: GrunnlagsKontekst): GrunnlagsdelResultat {
        val resultat = aaregClient.hentArbeidsForhold(kontekst.fnr)
        return GrunnlagsdelResultat(
            type = type,
            data = resultat.data,
            ok = resultat.statusCode in 200..299,
            status = resultat.statusCode,
            feilmelding = resultat.errorMessage
        )
    }
}
