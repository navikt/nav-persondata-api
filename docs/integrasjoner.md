# Integrasjoner

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
