package no.nav.persondataapi.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.persondataapi.aareg.client.Arbeidsforhold
import no.nav.persondataapi.aareg.client.Identtype
import no.nav.persondataapi.aareg.client.hentIdenter
import no.nav.persondataapi.domain.AaregResultat
import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.ereg.client.EregClient
import no.nav.persondataapi.ereg.client.EregRespons


import no.nav.persondataapi.service.dataproviders.GrunnlagsKontekst
import no.nav.persondataapi.service.dataproviders.GrunnlagsProvider
import no.nav.persondataapi.service.dataproviders.GrunnlagsType
import no.nav.security.token.support.core.context.TokenValidationContextHolder

import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OppslagService(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val tokenService: TokenService,
    private val providers: List<GrunnlagsProvider> , // injiseres automatisk av Spring
    private val eregClient: EregClient
) {

    suspend fun hentGrunnlagsData(
        fnr: String,
        typer: Set<GrunnlagsType> = GrunnlagsType.entries.toSet()
    ): GrunnlagsData {

        val context = tokenValidationContextHolder.getTokenValidationContext()
        val issuer = context.issuers.first()
        val claims = context.getClaims(issuer)
        val username = claims.getStringClaim("NAVident")

        println("Bruker $username gjorde oppslag på fnr: $fnr")

        val token = context.firstValidToken?.encodedToken
            ?: throw IllegalStateException("Fant ikke gyldig token")

        val kontekst = GrunnlagsKontekst(fnr, username, token)

        val resultater = coroutineScope {
            providers
                .filter { it.type in typer }
                .map { provider ->
                    async {
                        provider.hent(kontekst)
                    }
                }
                .awaitAll()
        }
        println("Alle svar hentet $resultater")

        val organiasajoner = mutableMapOf<String,EregRespons>()
        // Eksempel på hvordan du setter sammen full respons
        val utbetalinger = resultater
            .find { it.type == GrunnlagsType.UTBETALINGER }
        val personData = resultater
            .find { it.type == GrunnlagsType.PERSONDATA }
        val inntektData = resultater
            .find { it.type == GrunnlagsType.INNTEKT }
        val aaRegData = resultater
            .find { it.type == GrunnlagsType.ARBEIDSFORHOLD }
        if (aaRegData != null && aaRegData.data !== null) {
            val v = aaRegData.data as List<Arbeidsforhold>
            var identer = v.hentIdenter()
                .map { it.ident }

            identer.forEach {
                ident ->
                try {
                    val org = eregClient.hentOrganisasjon(ident)
                    organiasajoner.put(ident,org)
                }
                catch (t:Exception){
                    t.printStackTrace()
                }

            }

        }
        println("returnerer svar fra OppslagService")

        return GrunnlagsData(
            utreksTidspunkt = ZonedDateTime.now(),
            ident = fnr,
            saksbehandlerId = username,
            utbetalingRespons = utbetalinger,
            personDataRespons = personData,
            inntektDataRespons = inntektData,
            aAaregDataRespons = aaRegData,
            eregDataRespons = organiasajoner
        )
    }
}