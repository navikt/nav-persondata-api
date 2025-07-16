package no.nav.persondataapi.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.persondataapi.domain.GrunnlagsData

import no.nav.persondataapi.domain.UtbetalingResultat
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
    private val providers: List<GrunnlagsProvider>  // injiseres automatisk av Spring
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

        // Eksempel på hvordan du setter sammen full respons
        val utbetalinger = resultater
            .find { it.type == GrunnlagsType.UTBETALINGER }
        val personData = resultater
            .find { it.type == GrunnlagsType.PERSONDATA }
        println("returnerer svar fra OppslagService")
        return GrunnlagsData(
            utreksTidspunkt = ZonedDateTime.now(),
            ident = fnr,
            saksbehandlerId = username,
            utbetalingRespons = utbetalinger,
            personDataRespons = personData
        )
    }
}