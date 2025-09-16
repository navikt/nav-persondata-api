package no.nav.persondataapi.service

import no.nav.persondataapi.aareg.client.Arbeidsforhold
import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.ereg.client.EregRespons
import no.nav.persondataapi.rest.domain.AnsettelsesDetalj
import no.nav.persondataapi.rest.domain.ArbeidsgiverData
import no.nav.persondataapi.rest.domain.InntektInformasjon
import no.nav.persondataapi.rest.domain.OpenPeriode
import no.nav.persondataapi.rest.domain.OppslagBrukerRespons
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger(ResponsMappingService::class.java)
@Component
class ResponsMappingService {

    fun mapToMOppslagBrukerResponse(grunnlagsData: GrunnlagsData): OppslagBrukerRespons {
        return OppslagBrukerRespons(
            utrekkstidspunkt = LocalDateTime.now(),
            saksbehandlerIdent = grunnlagsData.saksbehandlerId,
            fødselsnummer = grunnlagsData.ident,
            personInformasjon = grunnlagsData.getPersonInformasjon(),
            arbeidsgiverInformasjon = grunnlagsData.getArbeidsGiverInformasjon(),
            inntektInformasjon = InntektInformasjon(lønnsinntekt = grunnlagsData.getLoennsinntektOversikt()),
            stønader = grunnlagsData.getStonadOversikt(),
        )

    }

}




fun mapArbeidsforholdTilArbeidsGiverData(arbeidsforhold: Arbeidsforhold,eregDataRespons: Map<String,EregRespons>): ArbeidsgiverData {
    val orgnummer = arbeidsforhold.hentOrgNummerTilArbeidsSted()
    return ArbeidsgiverData(eregDataRespons.orgNummerTilOrgNavn(orgnummer),
        orgnummer,
        eregDataRespons.orgnummerTilAdresse(orgnummer),
        ansettelsesDetaljer = arbeidsforhold.ansettelsesdetaljer.map
        {
                ansettelsesdetaljer -> AnsettelsesDetalj(
            ansettelsesdetaljer.type,
            ansettelsesdetaljer.avtaltStillingsprosent,
            ansettelsesdetaljer.antallTimerPrUke,
            OpenPeriode(ansettelsesdetaljer.rapporteringsmaaneder.fra,ansettelsesdetaljer.rapporteringsmaaneder.til),
                    ansettelsesdetaljer.yrke.beskrivelse
        ) },

        )


}
