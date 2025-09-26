package no.nav.persondataapi.service

import no.nav.persondataapi.aareg.client.Arbeidsforhold
import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.ereg.client.EregRespons
import no.nav.persondataapi.rest.domain.AnsettelsesDetalj
import no.nav.persondataapi.rest.domain.ArbeidsgiverData
import no.nav.persondataapi.rest.domain.InntektInformasjon
import no.nav.persondataapi.rest.domain.OpenPeriode
import no.nav.persondataapi.rest.domain.OppslagBrukerRespons
import no.nav.persondataapi.rest.domain.PersonInformasjon
import org.springframework.stereotype.Component
import java.time.LocalDateTime


@Component
class ResponsMappingService(
    private val kodeverkService: KodeverkService
) {
    fun mapToOppslagBrukerResponse(grunnlagsData: GrunnlagsData): OppslagBrukerRespons {
        return OppslagBrukerRespons(
            utrekkstidspunkt = LocalDateTime.now(),
            saksbehandlerIdent = grunnlagsData.saksbehandlerId,
            fødselsnummer = grunnlagsData.ident,
            personInformasjon = berikMedKodeverkData(grunnlagsData.getPersonInformasjon()),
            arbeidsgiverInformasjon = grunnlagsData.getArbeidsgiverInformasjon(),
            inntektInformasjon = InntektInformasjon(lønnsinntekt = grunnlagsData.getLønnsinntektOversikt()),
            stønader = grunnlagsData.getStonadOversikt(),
        )
    }

    private fun berikMedKodeverkData(input: PersonInformasjon): PersonInformasjon {
        return input.copy(
                statsborgerskap = input.statsborgerskap.map { kodeverkService.mapLandkodeTilLandnavn(it) ?: "Ukjent" },
                adresse = input.adresse.let { adresse ->
                    adresse?.copy(
                        utenlandskAdresse = adresse.utenlandskAdresse?.copy(
                            landkode = kodeverkService.mapLandkodeTilLandnavn(adresse.utenlandskAdresse.landkode)
                        )
                    )
                }

        )
    }
}




fun mapArbeidsforholdTilArbeidsgiverData(arbeidsforhold: Arbeidsforhold, eregDataRespons: Map<String,EregRespons>): ArbeidsgiverData {
    val orgnummer = arbeidsforhold.hentOrgNummerTilArbeidssted()
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
