package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.aareg.client.AaregClient
import no.nav.persondataapi.integrasjon.aareg.client.Arbeidsforhold
import no.nav.persondataapi.integrasjon.aareg.client.hentIdenter
import no.nav.persondataapi.integrasjon.ereg.client.EregClient
import no.nav.persondataapi.integrasjon.ereg.client.EregRespons
import no.nav.persondataapi.rest.domene.ArbeidsgiverInformasjon
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.rest.oppslag.maskerObjekt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ArbeidsforholdService(
    private val aaregClient: AaregClient,
    private val eregClient: EregClient,
    private val brukertilgangService: BrukertilgangService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentArbeidsforholdForPerson(personIdent: PersonIdent): ArbeidsforholdResultat {
        // Hent arbeidsforhold fra Aareg
        val aaregRespons = aaregClient.hentArbeidsforhold(personIdent)
        logger.info("Hentet arbeidsforhold for $personIdent, status ${aaregRespons.statusCode}")

        // Håndter feil fra Aareg
        when (aaregRespons.statusCode) {
            404 -> return ArbeidsforholdResultat.PersonIkkeFunnet
            403 -> return ArbeidsforholdResultat.IngenTilgang
            500 -> return ArbeidsforholdResultat.FeilIBaksystem
            !in 200..299 -> return ArbeidsforholdResultat.FeilIBaksystem
        }

        val alleArbeidsforhold = aaregRespons.data

        // Returner tom liste hvis ingen arbeidsforhold
        if (alleArbeidsforhold.isEmpty()) {
            logger.info("Fant ingen arbeidsforhold for $personIdent")
            return ArbeidsforholdResultat.Success(
                ArbeidsgiverInformasjon(
                    løpendeArbeidsforhold = emptyList(),
                    historikk = emptyList()
                )
            )
        }

        // Hent organisasjonsinformasjon fra Ereg
        val unikeOrganisasjonsnumre: Map<String, EregRespons> = alleArbeidsforhold
            .hentIdenter()
            .map { it.ident }
            .distinct()
            .associateWith { ident -> eregClient.hentOrganisasjon(ident) }

        // Skill mellom løpende og historiske arbeidsforhold
        val løpendeArbeidsforhold = alleArbeidsforhold
            .filter { it.ansettelsesperiode.sluttdato == null }
            .map { arbeidsforhold ->
                mapArbeidsforholdTilArbeidsgiverData(arbeidsforhold, unikeOrganisasjonsnumre)
            }

        val historiskeArbeidsforhold = alleArbeidsforhold
            .filter { it.ansettelsesperiode.sluttdato != null }
            .map { arbeidsforhold ->
                mapArbeidsforholdTilArbeidsgiverData(arbeidsforhold, unikeOrganisasjonsnumre)
            }

        logger.info("Fant ${løpendeArbeidsforhold.size} løpende og ${historiskeArbeidsforhold.size} historiske arbeidsforhold for $personIdent")

        var respons = ArbeidsgiverInformasjon(
            løpendeArbeidsforhold = løpendeArbeidsforhold,
            historikk = historiskeArbeidsforhold
        )

        if (!brukertilgangService.harSaksbehandlerTilgangTilPersonIdent(personIdent)) {
            logger.info("Saksbehandler har ikke tilgang til å hente arbeidsforhold for $personIdent. Maskerer responsen")
            respons = maskerObjekt(respons)
        }

        return ArbeidsforholdResultat.Success(respons)
    }

    private fun mapArbeidsforholdTilArbeidsgiverData(
        arbeidsforhold: Arbeidsforhold,
        eregDataRespons: Map<String, EregRespons>
    ): ArbeidsgiverInformasjon.ArbeidsgiverData {
        val orgnummer = arbeidsforhold.hentOrgNummerTilArbeidssted()
        return ArbeidsgiverInformasjon.ArbeidsgiverData(
            arbeidsgiver = eregDataRespons.orgNummerTilOrgNavn(orgnummer),
            organisasjonsnummer = orgnummer,
            adresse = eregDataRespons.orgnummerTilAdresse(orgnummer),
            ansettelsesDetaljer = arbeidsforhold.ansettelsesdetaljer.map { ansettelsesdetaljer ->
                ArbeidsgiverInformasjon.AnsettelsesDetalj(
                    type = ansettelsesdetaljer.type,
                    stillingsprosent = ansettelsesdetaljer.avtaltStillingsprosent,
                    antallTimerPrUke = ansettelsesdetaljer.antallTimerPrUke,
                    periode = ArbeidsgiverInformasjon.ÅpenPeriode(
                        fom = ansettelsesdetaljer.rapporteringsmaaneder.fra,
                        tom = ansettelsesdetaljer.rapporteringsmaaneder.til
                    ),
                    yrke = ansettelsesdetaljer.yrke.beskrivelse
                )
            },
        )
    }
}

sealed class ArbeidsforholdResultat {
    data class Success(val data: ArbeidsgiverInformasjon) : ArbeidsforholdResultat()
    data object IngenTilgang : ArbeidsforholdResultat()
    data object PersonIkkeFunnet : ArbeidsforholdResultat()
    data object FeilIBaksystem : ArbeidsforholdResultat()
}
