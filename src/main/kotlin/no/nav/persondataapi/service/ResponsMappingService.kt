package no.nav.persondataapi.service

import no.nav.persondataapi.aareg.client.Arbeidsforhold
import no.nav.persondataapi.aareg.client.Identtype
import no.nav.persondataapi.domain.AaregResultat
import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.ereg.client.EregRespons
import no.nav.persondataapi.rest.domain.AnsettelsesDetalj
import no.nav.persondataapi.rest.domain.ArbeidsgiverData
import no.nav.persondataapi.rest.domain.ArbeidsgiverInformasjon
import no.nav.persondataapi.rest.domain.OpenPeriode
import no.nav.persondataapi.rest.domain.OppslagBrukerRespons
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ResponsMappingService {

private val logger = LoggerFactory.getLogger(ResponsMappingService::class.java)


    fun mapToMOppslagBrukerResponse(grunnlagsData: GrunnlagsData): OppslagBrukerRespons {
        return OppslagBrukerRespons(
            LocalDateTime.now(),
            "",
            ""
        )

    }

}

fun GrunnlagsData.getArbeidsGiverInformasjon(): ArbeidsgiverInformasjon{

    if (this.aAaregDataRespons == null){
        return ArbeidsgiverInformasjon(
            emptyList(),emptyList()
        )
    }
    else{
        val aaregResultat: AaregResultat = this.aAaregDataRespons!!.data as AaregResultat
        val lopendeArbeidsforhold = aaregResultat.data.filter { it.ansettelsesperiode.sluttdato == null }
        val historiskeArbeidsforhold = aaregResultat.data.filter { it.ansettelsesperiode.sluttdato != null }
        /*
        * map løpende arbeidsforhold
        * */
        val lopende = lopendeArbeidsforhold.map { arbeidsforhold ->
            mapArbeidsforholdTilArbeidsGiverData(arbeidsforhold,this.eregDataRespons)
        }
        val historisk = historiskeArbeidsforhold.map { arbeidsforhold ->
            mapArbeidsforholdTilArbeidsGiverData(arbeidsforhold,this.eregDataRespons)
        }
        return ArbeidsgiverInformasjon(
            lopende,historisk
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
            OpenPeriode(ansettelsesdetaljer.rapporteringsmaaneder.fra,ansettelsesdetaljer.rapporteringsmaaneder.til)
        ) },

        )


}
fun Arbeidsforhold.hentOrgNummerTilArbeidsSted(): String {
    val identOrgNummer  = this.arbeidssted.identer.firstOrNull() { it.type == Identtype.ORGANISASJONSNUMMER }
    if (identOrgNummer == null){
        return "Ingen OrgNummer"
    }
    return identOrgNummer.ident
}


fun Map<String, EregRespons>.orgNummerTilOrgNavn(orgnummer:String): String {
    this.get(orgnummer)?.let {
       return  it.navn!!.sammensattnavn
    }
    return "INGEN NAVN"
}

fun Map<String, EregRespons>.orgnummerTilAdresse(orgnummer: String): String =
    this[orgnummer]
        ?.organisasjonDetaljer
        ?.forretningsadresser
        ?.firstOrNull { it.gyldighetsperiode.tom == null }
        ?.let { "${it.adresselinje1}, ${it.postnummer} ${it.poststed}" }
        ?: "INGEN ADRESSSE"