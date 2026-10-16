package no.nav.persondataapi.rest.domene

import io.swagger.v3.oas.annotations.media.Schema

/**
 * V1-responsformat for personopplysninger — brukes når feature-flagget watson-sok-v-1-2 er AV.
 *
 * Tilsvarer PersonInformasjon, men `familemedlemmer` er representert som `Map<String, String>` (ident → rolle)
 * istedenfor `List<Familiemedlem>`.
 */
data class PersonInformasjonV1Dto(
    val aktørId: String?,
    val familemedlemmer: Map<String, String>,
    val statsborgerskap: List<String>,
    val navn: PersonInformasjon.Navn,
    val adresse: PersonInformasjon.Bostedsadresse?,
    val sivilstand: String?,
    val alder: Int,
    val adressebeskyttelse: PersonInformasjon.Skjerming,
    val fødselsdato: String,
    val dødsdato: String?,
    val navKontor: PersonInformasjon.NavKontor?,
    @field:Schema(
        description = "Aktør-ID fra PDL. Returneres bare når kalleren har full tilgang.",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    @Suppress("ConstructorParameterNaming")
    val pdlAktørId: String? = null,
)

fun PersonInformasjon.tilV1Format(): PersonInformasjonV1Dto =
    PersonInformasjonV1Dto(
        aktørId = aktørId,
        familemedlemmer = familemedlemmer.associate { it.ident to it.rolle },
        statsborgerskap = statsborgerskap,
        navn = navn,
        adresse = adresse,
        sivilstand = sivilstand,
        alder = alder,
        adressebeskyttelse = adressebeskyttelse,
        fødselsdato = fødselsdato,
        dødsdato = dødsdato,
        navKontor = navKontor,
        pdlAktørId = pdlAktørId,
    )
