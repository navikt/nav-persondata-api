package no.nav.persondataapi.service

import io.micrometer.observation.annotation.Observed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.inntekt.generated.model.InntektshistorikkApiUt
import no.nav.persondataapi.aareg.client.Arbeidsforhold
import no.nav.persondataapi.aareg.client.hentIdenter
import no.nav.persondataapi.domain.AaregDataResultat
import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.domain.InntektDataResultat
import no.nav.persondataapi.domain.PersonDataResultat
import no.nav.persondataapi.domain.UtbetalingRespons
import no.nav.persondataapi.domain.UtbetalingResultat
import no.nav.persondataapi.ereg.client.EregClient
import no.nav.persondataapi.ereg.client.EregRespons
import no.nav.persondataapi.generated.hentperson.Person


import no.nav.persondataapi.service.dataproviders.GrunnlagsKontekst
import no.nav.persondataapi.service.dataproviders.GrunnlagsProvider
import no.nav.persondataapi.service.dataproviders.GrunnlagsType
import no.nav.persondataapi.utbetaling.dto.Utbetaling
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
@Observed(
    name = "persondataapi.oppslagService",
    contextualName = "hentGrunnlagsdata",
    lowCardinalityKeyValues = ["component","service"]
)
class OppslagService(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val providers: List<GrunnlagsProvider> , // injiseres automatisk av Spring
    private val eregClient: EregClient
) {
    private val log = LoggerFactory.getLogger(javaClass)
    // Create a marker that your Logback configuration recognizes for "team-logs"
    private val teamLogsMarker = MarkerFactory.getMarker("TEAM_LOGS")

    suspend fun hentGrunnlagsData(
        fnr: String,
        typer: Set<GrunnlagsType> = GrunnlagsType.entries.toSet()
    ): GrunnlagsData {

        val context = tokenValidationContextHolder.getTokenValidationContext()
        val issuer = context.issuers.first()
        val claims = context.getClaims(issuer)
        var username = claims.getStringClaim("NAVident")
        if (username == null){
            //Service user have been used!!
            username = claims.getStringClaim("azp_name")
        }
        log.info("Bruker $username gjorde oppslag på fnr: $fnr")

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
        log.info(teamLogsMarker,"Alle data hentet for $fnr")

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
                    log.error("Feil ved henting av informasjon for organisasjon $ident",t)
                }

            }

        }

        val utbetalingResultat = resultater.find { it.type == GrunnlagsType.UTBETALINGER }.let { utbetalingData -> UtbetalingResultat(utbetalingData?.data as UtbetalingRespons, utbetalingData?.status!!,utbetalingData.feilmelding) }
        val personDataResultat = resultater.find { it.type == GrunnlagsType.PERSONDATA }.let { personData -> PersonDataResultat(personData?.data as Person, personData?.status!!,personData.feilmelding)}
        val inntektDataResultat = resultater.find { it.type == GrunnlagsType.INNTEKT }.let { inntektData -> InntektDataResultat(inntektData?.data as InntektshistorikkApiUt, personData?.status!!,personData.feilmelding)}
        val arbeidDataResultat = resultater.find { it.type == GrunnlagsType.ARBEIDSFORHOLD }.let { arbeidData -> AaregDataResultat(arbeidData?.data as List<Arbeidsforhold>, arbeidData.status,arbeidData.feilmelding)}

        try {
            val respons = GrunnlagsData(
                utreksTidspunkt = ZonedDateTime.now(),
                ident = fnr,
                saksbehandlerId = username,
                utbetalingRespons = utbetalingResultat,
                personDataRespons = personDataResultat,
                inntektDataRespons = inntektDataResultat,
                aAaregDataRespons = arbeidDataResultat,
                eregDataRespons = organiasajoner
            )
            return respons
        }
        catch (t:Exception){
            log.error("Feil oppretting av respons objekt for $fnr",t)
        }
        val respons = GrunnlagsData(
            utreksTidspunkt = ZonedDateTime.now(),
            ident = fnr,
            saksbehandlerId = username,
            utbetalingRespons = utbetalinger?.data as UtbetalingResultat,
            personDataRespons = personData?.data as PersonDataResultat,
            inntektDataRespons = inntektData?.data as InntektDataResultat,
            aAaregDataRespons = aaRegData?.data as AaregDataResultat,
            eregDataRespons = organiasajoner
        )
        return respons
    }
}