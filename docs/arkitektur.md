# Arkitektur

## Formål og kontekst
- API-et gir saksbehandlere et helhetlig oppslag på persondata (personopplysninger, arbeidsforhold, inntekt, ytelser) med innebygget maskering og logging.
- Applikasjonen er en Spring Boot 3.5-applikasjon skrevet i Kotlin, med caching aktivert fra oppstarten (`Application.kt`).

## Lagdelt oppbygning
- **REST-lag** (`rest/oppslag`): Beskyttede POST-endepunkter per fagområde. Controllerne bruker `runBlocking` for å kalle suspenderende tjenestemetoder og returnerer typed DTO-er.
- **Tjenestelag** (`service`): Orkestrerer kall mot eksterne systemer, beriker data (kodeverk, NAV-kontor), håndterer feil og maskering. Eksempler: `PersonopplysningerService`, `ArbeidsforholdService`, `InntektService`, `YtelseService`.
- **Integrasjonslag** (`integrasjon/...`): WebClient-baserte klienter per system. PDL hentes med GraphQL-klienten, mens inntekt, Aareg, utbetaling, Ereg, kodeverk, Norg2 og Tilgangsmaskinen bruker REST/OpenAPI. `TokenService` leverer OBO- og client-credentials-tokens.
- **Domene** (`rest/domene`): Objekt-innpakninger som `PersonIdent` sikrer riktig maskering i `toString`, og felter annoteres med `@Maskert`. `MaskeringUtil` traverserer datastrukturer for å erstatte maskerte felt.

## Dataflyt ved et oppslag
1. **Innkommende kall**: `NavCallIdServletFilter` setter `Nav-Call-Id`, saksbehandlerens NAV-ident og valgfritt `logg`-flagg i MDC.
2. **Tilgangsvurdering**: Controlleren bruker `BrukertilgangService`/`TilgangService` som kombinerer Tilgangsmaskin-respons med AD-grupper for utvidet tilgang.
3. **Innhenting**: Tjenesten kaller integrasjonsklienter med korrelerte tokens og systemspesifikke headers (f.eks. Behandlingsnummer/Tema mot PDL). Standard WebClient-timeouts er satt i `WebClientConfig`.
4. **Beriking og maskering**: Kodeverk- og organisasjonsdata legges til, og `maskerObjekt` brukes dersom tilgangsavslag eller skjermingsnivå krever det.
5. **Sporbarhet**: Vellykede personoppslag auditeres via `RevisjonsloggService` (CEF-format), og tracing kan utløses ved å sende headeren `logg=true`.

## Sikkerhet og personvern
- Tokenvalidering håndteres av `SecurityConfiguration` med Azure AD-issuer og audience fra konfigurasjon. Alle eksterne dataforespørsler signeres med systemtoken eller OBO-token fra `TokenService`.
- Tilgangskontroll bygger på Tilgangsmaskinen, med AD-grupper for utvidet tilgang og geografisk overstyring for å unngå falske avslag.
- Maskering er deklarativ via `@Maskert` og brukes bredt på identifiserende felter. Høy skjerming eller manglende tilgang fører til maskert eller avvist svar.
- Audit-logg skrives i CEF-format mot egen logger for alle godkjente oppslag i `PersonbrukerController`.

## Ytelse og robusthet
- Cache-laget bruker Valkey/Redis i alle miljøer unntatt `local`, der Caffeine benyttes (`CacheConfiguration`). TTL og størrelse styres per cache i `application.yaml`, og `CacheAdminService` kan flushe alle eller per personident.
- GraphQL- og REST-kall caches selektivt (f.eks. `pdl-person`, `aareg-arbeidsforhold`, `inntekt-historikk`, `utbetaling-bruker`, `kodeverk-*`).
- WebClient-instansene har tidsavbrudd og lav-kardinalitet observasjonskonvensjoner satt i `WebClientConfig` og `ClientMetricsConfig` for å unngå metrikksstøy.
- `BaseDownstreamMetrics` tilbyr standard timere og tellere per integrasjon, eksponert via Prometheus/Actuator.

## Teknologistack og byggløp
- Kotlin 2.2, Spring Boot 3.5, Micrometer-tracing (OTLP), Reactor Netty, Caffeine/Redis/Valkey.
- Klienter genereres i byggløpet: GraphQL-klient for PDL fra `src/main/resources/graphql`, og OpenAPI-klient for inntekt fra `src/main/resources/openapi`. Genererte kilder legges til som egne `sourceSets`.
- Bygg og tester kjøres med `./gradlew clean build`. Lokal kjøring bruker profilen `local` og henter hemmeligheter via `./get-secrets.sh`.

## Arkitekturelle hovedvalg
- **Lagdelt Spring-arkitektur** med tydelig separasjon mellom controller, tjeneste, integrasjon og domene gjør det enkelt å teste og bytte ut eksterne avhengigheter.
- **GraphQL mot PDL** gir presise felthentinger, mens øvrige systemer benytter standard REST/OpenAPI for enkel klientgenerering.
- **Deklarativ maskering** gjennom `@Maskert` og objektoinnpakninger (f.eks. `PersonIdent`) reduserer risiko for utilsiktet logging og eksponering.
- **Delt cache-backend** via Valkey i sky og Caffeine lokalt balanserer ytelse, kost og enkel feilsøking.
- **Korrelerbarhet og audit** er bygget inn fra filteret til auditlogger, slik at alle oppslag kan spores på tvers av tjenester.
