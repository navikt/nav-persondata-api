package no.nav.persondataapi.service

import no.nav.inntekt.generated.model.HistorikkData
import no.nav.inntekt.generated.model.Inntektsinformasjon
import no.nav.inntekt.generated.model.Loennsinntekt
import no.nav.inntekt.generated.model.YtelseFraOffentlige
import no.nav.persondataapi.aareg.client.Arbeidsforhold
import no.nav.persondataapi.aareg.client.Identtype
import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.ereg.client.EregRespons
import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.generated.hentperson.UtenlandskAdresse
import no.nav.persondataapi.generated.hentperson.Vegadresse
import no.nav.persondataapi.rest.domain.NorskAdresse
import no.nav.persondataapi.rest.domain.ArbeidsgiverInformasjon
import no.nav.persondataapi.rest.domain.Bostedsadresse
import no.nav.persondataapi.rest.domain.LoensDetaljer
import no.nav.persondataapi.rest.domain.Navn
import no.nav.persondataapi.rest.domain.Periode
import no.nav.persondataapi.rest.domain.PeriodeInformasjon
import no.nav.persondataapi.rest.domain.PersonInformasjon
import no.nav.persondataapi.rest.domain.Stonad
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period

private val logger = LoggerFactory.getLogger(ResponsMappingService::class.java)

fun Arbeidsforhold.hentOrgNummerTilArbeidsSted(): String {
    val identOrgNummer  = this.arbeidssted.identer.firstOrNull() { it.type == Identtype.ORGANISASJONSNUMMER }
    if (identOrgNummer == null){
        return "Ingen OrgNummer"
    }
    return identOrgNummer.ident
}

fun Map<String, EregRespons>.orgNummerTilOrgNavn(orgnummer:String): String {
    val organisasjon = this[orgnummer]
    if (organisasjon == null){
        return "${orgnummer} - Ukjent organisasjon"
    }
    else {
        return organisasjon.navn?.sammensattnavn ?: "${orgnummer} - Ukjent navn"
    }

}

fun GrunnlagsData.getLoennsinntektOversikt(): List<LoensDetaljer> {
    if (this.inntektDataRespons==null || this.inntektDataRespons.data==null){
        return emptyList()
    }
    else {
        val listeAvInntektHistorikk = inntektDataRespons.data.data?: emptyList()

        val  ret: MutableList<LoensDetaljer> = mutableListOf()
        listeAvInntektHistorikk.forEach { historikk ->
            val harHistorikkPaaLoennsinntekt = historikk.historikkPaaNormalLoenn()
            println(harHistorikkPaaLoennsinntekt)
            val nyeste = historikk.versjoner.nyeste()
            if (nyeste !=null && nyeste.inntektListe!=null){
                val liste = nyeste.inntektListe ?: emptyList()
                liste.filter { it is Loennsinntekt }.map { it as Loennsinntekt }.forEach { loenn ->
                    ret.add(LoensDetaljer(
                        arbeidsgiver = this.eregDataRespons.orgNummerTilOrgNavn(historikk.opplysningspliktig),
                        periode = historikk.maaned,
                        arbeidsforhold = "",
                        stillingsprosent = "",
                        lønnstype = loenn.beskrivelse,
                        antall = loenn.antall,
                        beløp = loenn.beloep,
                        harFlereVersjoner = harHistorikkPaaLoennsinntekt,
                    ))
                }
            }


        }

        return ret
    }
}

fun Map<String, EregRespons>.orgnummerTilAdresse(orgnummer: String): String =
    this[orgnummer]
        ?.organisasjonDetaljer
        ?.forretningsadresser
        ?.firstOrNull { it.gyldighetsperiode.tom == null }
        ?.let { "${it.adresselinje1}, ${it.postnummer}" }
        ?: "INGEN ADRESSSE"

fun Person.fulltNavn(): String {
    val navn = this.navn.first()
    return "${navn.fornavn} ${navn.mellomnavn} ${navn.etternavn}"
}

fun Person.gjeldendeFornavn(): String {
    val navn = this.navn.first()
    return navn.fornavn
}

fun Person.gjeldendeSivilStand(): String {
    val sivilstand = this.sivilstand.first()
    return sivilstand.type.name
}
fun Person.gjeldendeMellomnavn(): String? {
    val navn = this.navn.first()
    return navn.mellomnavn
}
fun Person.gjeldendeEtternavn(): String {
    val navn = this.navn.first()
    return navn.etternavn
}

fun Person.naavarendeBostedsAdresseToString():String{
    val adresse = this.bostedsadresse.first()
    //har bruker utlands adresse
    if (adresse.utenlandskAdresse!= null){
        return adresse.utenlandskAdresse!!.fullAdresseString()
    }
    if (adresse.vegadresse!= null){
        return adresse.vegadresse!!.fullAdresseString()
    }

    return "Ingen adresse registrert"
}

fun Person.naavarendeBostedsAdresse(): no.nav.persondataapi.rest.domain.Bostedsadresse{
    val adresse = this.bostedsadresse.first()
    //har bruker utlands adresse
    var utlandAdresse: no.nav.persondataapi.rest.domain.UtenlandskAdresse? = null
    var norskAdresse: no.nav.persondataapi.rest.domain.NorskAdresse? = null
    if (adresse.utenlandskAdresse!= null){
        utlandAdresse = no.nav.persondataapi.rest.domain.UtenlandskAdresse(
            adressenavnNummer = adresse.utenlandskAdresse.adressenavnNummer,
            bygningEtasjeLeilighet = adresse.utenlandskAdresse.bygningEtasjeLeilighet,
            postboksNummerNavn = null,
            postkode = null,
            bySted = null,
            regionDistriktOmråde = null,
            landkode = "null"
        )
    }
    if (adresse.vegadresse!= null){
        norskAdresse = NorskAdresse(
            adressenavn = adresse.vegadresse.adressenavn,
            husnummer = adresse.vegadresse.husnummer,
            husbokstav = adresse.vegadresse.husbokstav,
            postnummer = adresse.vegadresse.postnummer,
            kommunenummer = adresse.vegadresse.kommunenummer,
            poststed = adresse.vegadresse.postnummer
        )
    }

    return no.nav.persondataapi.rest.domain.Bostedsadresse(
        norskAdresse = norskAdresse,
        utenlandskAdresse = utlandAdresse
    )
}

fun UtenlandskAdresse.fullAdresseString():String{
    return "$this."
}
fun Vegadresse.fullAdresseString():String{
    if (this.husbokstav!=null)
        return "${this.adressenavn} ${this.husnummer}${this.husbokstav}, ${this.postnummer}"
    else return "${this.adressenavn} ${this.husnummer}, ${this.postnummer}"
}


fun List<Inntektsinformasjon>?.nyeste(): Inntektsinformasjon?{
    return if (this==null || this.isEmpty() )
    {
        null
    }
    else{
        this.minByOrNull { it.oppsummeringstidspunkt }!!
    }
}




fun HistorikkData.historikkPaaNormalLoenn():Boolean{
    val versjoner = this.versjoner?:emptyList()
    var count = 0
    versjoner.forEach {
            intektInformasjon ->
        val inntektListe = intektInformasjon.inntektListe?:emptyList()
        val antall = inntektListe.filterNot { inntekt -> inntekt is YtelseFraOffentlige }.size
        if (antall>0) count++
    }
    return count>1
}


fun GrunnlagsData.getStonadOversikt(): List<Stonad> {
    if (this.utbetalingRespons==null){
        logger.warn("ingen utbetalingRespons for ${this.ident}")
        return emptyList()
    }
    else{
        val utbetalinger = this.utbetalingRespons!!.data!!.utbetalinger
        val ytelser = utbetalinger.flatMap { it.ytelseListe.map { it.ytelsestype } }.distinct()



        val stonadListe = mutableListOf<Stonad>()
        ytelser.forEach {
                ytelser ->
            val ytelseListe = utbetalinger.flatMap { it.ytelseListe }.filter { it.ytelsestype==ytelser }
            val listOfPeriodeInformasjon = mutableListOf<PeriodeInformasjon>()
            ytelseListe.forEach {ytelser ->
                val info = PeriodeInformasjon(
                    periode = Periode(fom = ytelser.ytelsesperiode.fom, tom = ytelser.ytelsesperiode.tom),
                    ytelser.ytelseNettobeloep,
                    kilde= "SOKOS",
                    info = ytelser.bilagsnummer
                )
                listOfPeriodeInformasjon.add(info)
            }
            stonadListe.add(Stonad(stonadType = ytelser!!,listOfPeriodeInformasjon))
        }
        return stonadListe
    }


}

fun GrunnlagsData.getArbeidsgiverInformasjon(): ArbeidsgiverInformasjon{

    if (this.aAaregDataRespons == null){
        return ArbeidsgiverInformasjon(
            emptyList(),emptyList()
        )
    }
    else{
        val aaregDataResultat = this.aAaregDataRespons!!.data
        val lopendeArbeidsforhold = aaregDataResultat.filter { it.ansettelsesperiode.sluttdato == null }
        val historiskeArbeidsforhold = aaregDataResultat.filter { it.ansettelsesperiode.sluttdato != null }
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

fun GrunnlagsData.getPersonInformasjon(): PersonInformasjon {

    if (this.personDataRespons == null){
        return PersonInformasjon(
            navn = Navn("",null,""),
            aktørId = this.ident,
            adresse = Bostedsadresse(null, null),
            familemedlemmer = emptyMap(),
            alder = -1
        )
    }
    else{

        val pdlResultat:Person  = this.personDataRespons!!.data as Person
        val foreldreOgBarn = pdlResultat.forelderBarnRelasjon.associate { Pair(it.relatertPersonsIdent!!, it.relatertPersonsRolle.name) }
        val foreldreansvar = pdlResultat.foreldreansvar.associate { Pair(it.ansvarssubjekt!!, "BARN") }
        val statsborgerskap = pdlResultat.statsborgerskap.map { it.land }
        val ektefelle = pdlResultat.sivilstand.filter { it.relatertVedSivilstand!=null }.associate { Pair(it.relatertVedSivilstand!!,it.type.name)}
        val foreldreOgBarnOgEktefelle: Map<String, String> = foreldreOgBarn + ektefelle
        return PersonInformasjon(
            navn = Navn(
                pdlResultat.gjeldendeFornavn(),
                mellomnavn = pdlResultat.gjeldendeMellomnavn(),
                etternavn = pdlResultat.gjeldendeEtternavn(),
            ),
            aktørId = this.ident,
            adresse = pdlResultat.naavarendeBostedsAdresse(),
            familemedlemmer = foreldreOgBarnOgEktefelle,
            statsborgerskap = statsborgerskap,
            sivilstand = pdlResultat.gjeldendeSivilStand(),
            alder = pdlResultat.foedselsdato.first().foedselsdato?.let { Period.between(LocalDate.parse(it), LocalDate.now()).years } ?: -1,
        )
    }

}
