package no.nav.persondataapi.service.dataproviders

import no.nav.persondataapi.inntekt.client.InntektClient
import org.springframework.stereotype.Component

@Component
class InntektGrunnlagProvider(val inntektClient: InntektClient) : GrunnlagsProvider {
    override val type = GrunnlagsType.INNTEKT
    override suspend fun hent(kontekst: GrunnlagsKontekst): GrunnlagsdelResultat
    {
        val resultat = inntektClient.hentInntekter(kontekst.fnr,kontekst.token)
        return GrunnlagsdelResultat(
            type = type,
            data = resultat.data,
            ok = resultat.statusCode in 200..299,
            status = resultat.statusCode,
            feilmelding = resultat.errorMessage
        )
    }
}