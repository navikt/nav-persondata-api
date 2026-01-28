# Integrasjoner


Innhold:

- [Innlogging og autentisering og henting av data for visning i Watson søk](#innlogging-og-autentisering-og-henting-av-data-for-visning-i-watson-søk)
- [Autentisering og felles oppsett](#autentisering-og-felles-oppsett)
- [PDL (Persondata)](#pdl-persondata)
- [NOM (Ressurs og organisasjon)](#nom-ressurs-og-organisasjon)
- [Tilgangsmaskinen](#tilgangsmaskinen)
- [Inntekt (IKOMP)](#inntekt-ikomp)
- [Utbetaling](#utbetaling)
- [Dagpenger datadeling (Meldekort)](#dagpenger-datadeling-meldekort)
- [Aareg (Arbeidsforhold)](#aareg-arbeidsforhold)
- [Ereg (Organisasjon)](#ereg-organisasjon)
- [Kodeverk](#kodeverk)
- [Norg2 (Enhetsinformasjon)](#norg2-enhetsinformasjon)
- [Modia Context Holder](#modia-context-holder)
- [Logging, tracing og metrics](#logging-tracing-og-metrics)


## Innlogging og autentisering og henting av data for visning i Watson søk
Denne tegningen gir en overordnet oversikt over hvordan innlogging, autentisering og
henting av persondata skjer i nav-persondata-api for visning i Watson søk.
Sekvensene er numereret med interaksjoner tiknyttet steget.
1. Saksbehandler logger inn i Watson søk, som henter token fra AAD.
2. sjekk av saksbehandlers tilgang til persondata-api:
   2.1 nav-persondata-api mottar forespørsel med token
   2.2 nav-persondata-api sender personident og token til nav-persondata-api
   2.3 nav-persondata-api sjekker om saksbehandler har tilgang til å hente data for personidenten
   2.4 nav-persondata-api returnerer tilgangssvar (403 Forbidden ved avslag)
3. Henting av informasjon tiknyttet personident:
   3.1 nav-persondata-api mottar forespørsel om henting av persondata for personident
   3.2 nav-persondata-api sjekker saksbehandlers tilgang til persondata for personident
   3.3 nav-persondata-api logger avslag/godkjenning av tilgang
   3.4 nav-persondata-api sjekker med tilgangsmaskinen om saksbehandler har tilgang til personident
   3.5 nav-persondata-api returnerer tilgangssvar (403 Forbidden ved avslag)
4. Henting av persondata ved godkjent tilgang:
   4.1 nav-persondata-api henter vedtaksdata fra ulike kilder:
   4.2 Hent utbetalinger fra Utbetaling
   4.3 Hent ytelser fra Ytelser
   4.4 Hent arbeidsforhold fra Arbeidsforhold
   4.5 Hent meldekortdata fra DAG, AAP, TIL
   4.6 nav-persondata-api returnerer data eller feil til Watson søk

Det vil tilkomme fler datakilder for saksbehandler i fremtiden.
````mermaid
flowchart TD
    A[Watson søk] -->|1. Logg inn saksbehandler| D[Hent fra AAD]
    A -->|2.2 Forespørsel personident| B[nav-persondata-api]
    B -->|2.3 Sjekk om saksbehandler har tilgang| C{Sjekk tilganger}
    B -->|2.4 tilgangssvar| A[Returner 403 Forbidden]
    B -->|3.1 Hent persondata| C{Sjekk tilganger}
    C -->|3.2 Logg tilgangsforespørsel| G[Audit logg]
    C -->|3.3 Logg avslag/godkjenning| G[Audit logg]
    C -->|3.4 Sjekk tilgang til personident| E[Tilgangsmaskinen]
    C --->|3.5 Returner Tilgangs svar|B
    B -->|4.0 Hent person i PDL|P[PDL]
    B -->|4.1 Hent data|F{Hent vedtaksdata}
    F --->|4.2 Hent utbetalinger| I[Utbetaling]
    F --->|4.3 Hent ytelser| J[Ytelser]
    F --->|4.4 Hent arbeidsforhold| K[Arbeidsforhold]
    F --->|4.5 Hent Meldekort| L[DAG, AAP, TIL]
    B -->|4.6 Returner data eller feil| A
    B -->|5.1 Logg nødvendige feilmeldinger| M{Applikasjonslogging}
    M -->|5.2 Logg sensitiv data|N[Secure logg]
    M -->|5.3 Logg generell info|O[Applikasjonslogg]
    B -->|5.4 Feilmeldding|A
    C -->| Logg tilgangsforespørsel| G[Audit logg]

    
    
````

## Autentisering og felles oppsett
- Alle nedstrømskall bruker WebClient-konfigurasjon fra `WebClientConfig`, som setter Call-Id-header, tidsavbrudd og Micrometer-observasjoner.
- Tokens hentes via `TokenService`:
  - Client credentials mot Azure AD for system-til-system (`getServiceToken`).
  - OBO-token for sluttbruker-kontekst når det trengs (`exchangeToken`).
- Scopes og endepunkter kommer fra miljøvariabler (`application.yaml` og `.env.local.properties`), og oversettes til URL-er for hver klient.

## PDL (Persondata)
- Protokoll: GraphQL via Expediagroup-klient.
- Endepunkt: `${PDL_URL}` (`/graphql`).
- Headers: `Authorization: Bearer <token>`, `behandlingsnummer=B634`, `TEMA=KTR`.
- Cache: `pdl-person` og `pdl-geografisktilknytning`.
- Bruk: Henter persondata og geografisk tilknytning for oppslag og NAV-kontorberikelse.

## NOM (Ressurs og organisasjon)
- Protokoll: GraphQL via Expediagroup-klient.
- Endepunkt: `${NOM_URL}`.
- Auth: Client credentials med `NOM_SCOPE`.
- Cache: `nom-ressurs` (per NAV-ident).
- Bruk: Henter saksbehandlers organisasjonstilhørighet til `/meg` og metrikker.

## Tilgangsmaskinen
- Protokoll: REST.
- Endepunkt: `${TILGANGMASKIN_URL}`.
- Auth: Client credentials med `TILGANGMASKIN_SCOPE`.
- Cache: Ingen eksplisitt cache; svar vurderes i `TilgangService`.
- Bruk: Vurderer tilgang, kombineres med AD-grupper for utvidet tilgang.

## Inntekt (IKOMP)
- Protokoll: REST (OpenAPI-generert).
- Endepunkt: `${INNTEKT_URL}`.
- Auth: Client credentials med `INNTEKT_SCOPE`.
- Cache: `inntekt-historikk`.
- Bruk: Henter inntektshistorikk for valgt ident.

## Utbetaling
- Protokoll: REST.
- Endepunkt: `${UTBETALING_URL}/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern`.
- Auth: Client credentials med `UTBETALING_SCOPE`.
- Cache: `utbetaling-bruker`.
- Bruk: Henter utbetalingsinformasjon for innbygger.

## Dagpenger datadeling (Meldekort)
- Protokoll: REST.
- Endepunkt: `${DP_DATADELING_URL}/dagpenger/datadeling/v1/meldekort`.
- Auth: Client credentials med `DP_DATADELING_SCOPE`.
- Cache: `meldekort` (nøkkel per ident og utvidet-flagget).
- Bruk: Henter dagpenge-meldekort for oppslag, med periode på 3 år eller 10 år ved utvidet visning.

## Aareg (Arbeidsforhold)
- Protokoll: REST.
- Endepunkt: `${AAREG_URL}`.
- Auth: Client credentials med `AAREG_SCOPE`.
- Cache: `aareg-arbeidsforhold`.
- Bruk: Henter arbeidsforhold og arbeidsgiverinformasjon.

## Ereg (Organisasjon)
- Protokoll: REST.
- Endepunkt: `${EREG_URL}`.
- Auth: Bruker samme tokenoppsett som øvrige systemer (Azure client credentials).
- Cache: `ereg-organisasjon`.
- Bruk: Beriker arbeidsgiverdata med navn og organisasjonsinfo.

## Kodeverk
- Protokoll: REST.
- Endepunkt: `${KODEVERK_URL}`.
- Auth: Client credentials med `KODEVERK_SCOPE`.
- Cache: `kodeverk-landkoder`, `kodeverk-postnummer`.
- Bruk: Mapper landkoder og postnummer til lesbare navn.

## Norg2 (Enhetsinformasjon)
- Protokoll: REST.
- Endepunkt: `${NORG2_URL}`.
- Auth: Azure client credentials.
- Cache: Ingen eksplisitt cache.
- Bruk: Henter enhetsinformasjon og grenser for NAV-kontor.

## Modia Context Holder
- Protokoll: REST.
- Endepunkt: `${MODIA_CONTEXT_HOLDER_URL}`.
- Auth: Client credentials med `MODIA_CONTEXT_HOLDER_SCOPE`.
- Cache: Ingen eksplisitt cache.
- Bruk: Oppdaterer Modia-kontekst etter vellykket oppslag.

## Logging, tracing og metrics
- Call-id propagasjon via `NavCallIdServletFilter` og `navCallIdHeaderFilter` gir korrelasjon på tvers av tjenester.
- Micrometer-observasjoner per klient (konvensjoner i `ClientMetricsConfig`) og standard tellere/timere (`BaseDownstreamMetrics`).
- Audit-logg i CEF-format for godkjente personoppslag via `RevisjonsloggService`.
