package no.nav.persondataapi.service

import no.nav.inntekt.generated.model.HistorikkData
import no.nav.inntekt.generated.model.Inntektsinformasjon
import no.nav.inntekt.generated.model.YtelseFraOffentlige
import no.nav.persondataapi.generated.pdl.hentperson.Person
import no.nav.persondataapi.integrasjon.aareg.client.Arbeidsforhold
import no.nav.persondataapi.integrasjon.aareg.client.Identtype
import no.nav.persondataapi.integrasjon.ereg.client.EregRespons
import no.nav.persondataapi.rest.domene.PersonInformasjon
import java.time.LocalDate

fun Arbeidsforhold.hentOrgNummerTilArbeidssted(): String {
    val identOrgNummer = this.arbeidssted.identer.firstOrNull { it.type == Identtype.ORGANISASJONSNUMMER }
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
        organisasjon.navn?.sammensattnavn ?: "$orgnummer - Ukjent navn"
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

fun Person.nåværendeBostedsadresse(): PersonInformasjon.Bostedsadresse? {
    val adresse = this.bostedsadresse.firstOrNull() ?: return null
    val utenlandskAdresse = adresse.utenlandskAdresse
    val vegadresse = adresse.vegadresse

    var utlandAdresse: PersonInformasjon.UtenlandskAdresse? = null
    var norskAdresse: PersonInformasjon.NorskAdresse? = null

    if (utenlandskAdresse != null) {
        utlandAdresse =
            PersonInformasjon.UtenlandskAdresse(
                adressenavnNummer = utenlandskAdresse.adressenavnNummer,
                bygningEtasjeLeilighet = utenlandskAdresse.bygningEtasjeLeilighet,
                postboksNummerNavn = utenlandskAdresse.postboksNummerNavn,
                postkode = utenlandskAdresse.postkode,
                bySted = utenlandskAdresse.bySted,
                regionDistriktOmråde = utenlandskAdresse.regionDistriktOmraade,
                landkode = utenlandskAdresse.landkode,
            )
    }
    if (vegadresse != null) {
        norskAdresse =
            PersonInformasjon.NorskAdresse(
                adressenavn = vegadresse.adressenavn,
                husnummer = vegadresse.husnummer,
                husbokstav = vegadresse.husbokstav,
                postnummer = vegadresse.postnummer,
                kommunenummer = vegadresse.kommunenummer,
                poststed = vegadresse.postnummer,
            )
    }

    return PersonInformasjon.Bostedsadresse(
        norskAdresse = norskAdresse,
        utenlandskAdresse = utlandAdresse,
    )
}

fun List<Inntektsinformasjon>?.nyeste(): Inntektsinformasjon? = this?.maxByOrNull { it.oppsummeringstidspunkt }

fun List<Inntektsinformasjon>?.eldste(): Inntektsinformasjon? = this?.minByOrNull { it.oppsummeringstidspunkt }!!

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

fun Person.telefonnummer(): List<PersonInformasjon.Telefonnummer> =
    this.telefonnummer
        .filter { !it.metadata.historisk }
        .sortedBy { it.prioritet }
        .map {
            PersonInformasjon.Telefonnummer(
                landskode = it.landskode,
                nummer = it.nummer,
                prioritet = it.prioritet,
            )
        }

fun Person.adresseHistorikkSiste5År(): List<PersonInformasjon.HistoriskAdresse> {
    val cutoff = LocalDate.now().minusYears(5)
    return this.bostedsadresse
        .filter { adresse ->
            val tilDato = adresse.gyldigTilOgMed?.let { LocalDate.parse(it) }
            tilDato == null || tilDato.isAfter(cutoff)
        }.mapNotNull { adresse ->
            val vegadresse = adresse.vegadresse
            val utenlandskAdresse = adresse.utenlandskAdresse

            val norskAdresse =
                vegadresse?.let {
                    PersonInformasjon.NorskAdresse(
                        adressenavn = it.adressenavn,
                        husnummer = it.husnummer,
                        husbokstav = it.husbokstav,
                        postnummer = it.postnummer,
                        kommunenummer = it.kommunenummer,
                        poststed = it.postnummer,
                    )
                }
            val utlandAdresse =
                utenlandskAdresse?.let {
                    PersonInformasjon.UtenlandskAdresse(
                        adressenavnNummer = it.adressenavnNummer,
                        bygningEtasjeLeilighet = it.bygningEtasjeLeilighet,
                        postboksNummerNavn = it.postboksNummerNavn,
                        postkode = it.postkode,
                        bySted = it.bySted,
                        regionDistriktOmråde = it.regionDistriktOmraade,
                        landkode = it.landkode,
                    )
                }

            if (norskAdresse == null && utlandAdresse == null) return@mapNotNull null

            PersonInformasjon.HistoriskAdresse(
                adresse = PersonInformasjon.Bostedsadresse(norskAdresse, utlandAdresse),
                gyldigFraOgMed = adresse.gyldigFraOgMed,
                gyldigTilOgMed = adresse.gyldigTilOgMed,
            )
        }
}
