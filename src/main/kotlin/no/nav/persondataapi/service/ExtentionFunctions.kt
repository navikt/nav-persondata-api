package no.nav.persondataapi.service

import no.nav.inntekt.generated.model.HistorikkData
import no.nav.inntekt.generated.model.Inntektsinformasjon
import no.nav.inntekt.generated.model.YtelseFraOffentlige
import no.nav.persondataapi.integrasjon.aareg.client.Arbeidsforhold
import no.nav.persondataapi.integrasjon.aareg.client.Identtype
import no.nav.persondataapi.integrasjon.ereg.client.EregRespons
import no.nav.persondataapi.generated.pdl.hentperson.Person
import no.nav.persondataapi.rest.domene.PersonInformasjon

fun Arbeidsforhold.hentOrgNummerTilArbeidssted(): String {
    val identOrgNummer = this.arbeidssted.identer.firstOrNull() { it.type == Identtype.ORGANISASJONSNUMMER }
    if (identOrgNummer == null) {
        return "Ingen OrgNummer"
    }
    return identOrgNummer.ident
}

fun Map<String, EregRespons>.orgNummerTilOrgNavn(orgnummer: String): String {
    val organisasjon = this[orgnummer]
    return if (organisasjon == null) {
        "$orgnummer - Ukjent organisasjon"
    } else {
        organisasjon.navn?.sammensattnavn ?: "${orgnummer} - Ukjent navn"
    }

}

fun Person.gjeldendeFornavn(): String {
    val navn = this.navn.firstOrNull() ?: return ""
    return navn.fornavn
}

fun Person.gjeldendeSivilStand(): String {
    val sivilstand = this.sivilstand.firstOrNull() ?: return "UKJENT"
    return sivilstand.type.name
}

fun Person.gjeldendeMellomnavn(): String? {
    val navn = this.navn.firstOrNull() ?: return null
    return navn.mellomnavn
}

fun Person.gjeldendeEtternavn(): String {
    val navn = this.navn.firstOrNull() ?: return ""
    return navn.etternavn
}

fun Person.nåværendeBostedsadresse(): PersonInformasjon.Bostedsadresse?  {
    val adresse = this.bostedsadresse.firstOrNull() ?: return null
    val utenlandskAdresse = adresse.utenlandskAdresse
    val vegadresse = adresse.vegadresse

    var utlandAdresse: PersonInformasjon.UtenlandskAdresse? = null
    var norskAdresse: PersonInformasjon.NorskAdresse? = null

    if (utenlandskAdresse != null) {
        utlandAdresse = PersonInformasjon.UtenlandskAdresse(
            adressenavnNummer = utenlandskAdresse.adressenavnNummer,
            bygningEtasjeLeilighet = utenlandskAdresse.bygningEtasjeLeilighet,
            postboksNummerNavn = utenlandskAdresse.postboksNummerNavn,
            postkode = utenlandskAdresse.postkode,
            bySted = utenlandskAdresse.bySted,
            regionDistriktOmråde = utenlandskAdresse.regionDistriktOmraade,
            landkode = utenlandskAdresse.landkode
        )
    }
    if (vegadresse != null) {
        norskAdresse = PersonInformasjon.NorskAdresse(
            adressenavn = vegadresse.adressenavn,
            husnummer = vegadresse.husnummer,
            husbokstav = vegadresse.husbokstav,
            postnummer = vegadresse.postnummer,
            kommunenummer = vegadresse.kommunenummer,
            poststed = vegadresse.postnummer
        )
    }

    return PersonInformasjon.Bostedsadresse(
        norskAdresse = norskAdresse,
        utenlandskAdresse = utlandAdresse
    )
}

fun List<Inntektsinformasjon>?.nyeste(): Inntektsinformasjon? {

    return this?.maxByOrNull { it.oppsummeringstidspunkt }
}
fun List<Inntektsinformasjon>?.eldste(): Inntektsinformasjon? {

        return this?.minByOrNull { it.oppsummeringstidspunkt }!!

}


fun HistorikkData.harHistorikkPåNormallønn(): Boolean {
    val versjoner = this.versjoner ?: emptyList()
    var count = 0

    versjoner.forEach { inntektInformasjon ->
        val inntektListe = inntektInformasjon.inntektListe ?: emptyList()
        val antall = inntektListe.filterNot { inntekt -> inntekt is YtelseFraOffentlige }.size
        if (antall > 0) count++
    }
    return count > 1
}
