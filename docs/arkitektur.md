# Arkitektur

## Formål og kontekst
- API-et gir saksbehandlere et helhetlig oppslag på persondata (personopplysninger, arbeidsforhold, inntekt, ytelser) med innebygget maskering og logging.
- Applikasjonen er en Spring Boot 3.5-applikasjon skrevet i Kotlin, med caching aktivert fra oppstarten (`Application.kt`).

## Integrasjoner og dataflyt

graph TD

A[Persondata-api] -->|REST API| B[OBO-veksling og client-credentials]
A[Persondata-api] -->|DOMENE| C[Personoppslag]
A[Persondata-api] -->|GraphQL| D[PDL]
A[Persondata-api] -->|Oppslag| E[Inntekt]
A[Persondata-api] -->|Oppslag| F[Arbeidsforhold]
A[Persondata-api] -->|Oppslag| G[Utbetalinger]
A[Persondata-api] -->|Oppslag| H[Tilgangsmaskinen]

- WebClient-instansene konfigureres sentralt med Call-ID-filter, Micrometer-merking og systemspesifikke observation-konvensjoner (`src/main/kotlin/no/nav/persondataapi/konfigurasjon/WebClientConfig.kt:42`, `ClientMetricsConfig.kt:19`).
- `TokenService` håndterer både OBO-veksling og client-credentials mot NAVs sikkerhetsplattform basert på miljøvariabler (`src/main/kotlin/no/nav/persondataapi/service/TokenService.kt:19`).
- PDL-data hentes via GraphQL med caching og tilpassede headers (`src/main/kotlin/no/nav/persondataapi/integrasjon/pdl/client/PdlClient.kt:22`); kodeverk- og organisasjonsdata hentes og brukes til beriking (`src/main/kotlin/no/nav/persondataapi/service/KodeverkService.kt:4`, `integrasjon/kodeverk/client/KodeverkClient.kt:27`, `integrasjon/ereg/client/EregClient.kt:16`).
- Inntekt, arbeidsforhold og utbetalinger konsumeres via egne klienter og mappes til interne domeneobjekter (`src/main/kotlin/no/nav/persondataapi/integrasjon/inntekt/client/InntektClient.kt:28`, `integrasjon/aareg/client/AaregClient.kt:28`, `integrasjon/utbetaling/client/UtbetalingClient.kt:23`).
- Tilgangskontroll mot Tilgangsmaskinen evalueres og brukes til å bestemme maskering, kombinert med lokale AD-grupper (`src/main/kotlin/no/nav/persondataapi/integrasjon/tilgangsmaskin/client/TilgangsmaskinClient.kt:25`, `service/TilgangService.kt:21`, `domene/Grupper.kt:14`).

## Dataflyt ved et oppslag
1. **Innkommende kall**: `NavCallIdServletFilter` setter `Nav-Call-Id`, saksbehandlerens NAV-ident og valgfritt `logg`-flagg i MDC.
2. **Tilgangsvurdering**: Controlleren bruker `BrukertilgangService`/`TilgangService` som kombinerer Tilgangsmaskin-respons med AD-grupper for utvidet tilgang.
3. **Innhenting**: Tjenesten kaller integrasjonsklienter med korrelerte tokens og systemspesifikke headers (f.eks. Behandlingsnummer/Tema mot PDL). Standard WebClient-timeouts er satt i `WebClientConfig`.
4. **Beriking og maskering**: Kodeverk- og organisasjonsdata legges til, og `maskerObjekt` brukes dersom tilgangsavslag eller skjermingsnivå krever det.
5. **Sporbarhet**: Vellykkede personoppslag audit-logges via `RevisjonsloggService` (CEF-format), og tracing kan utløses ved å sende headeren `logg=true`.

## Bygg og konfig
- <!-- versions --> GraphQL- og OpenAPI-klienter genereres som del av kompilasjonsløpet (`build.gradle.kts:6`, `build.gradle.kts:16`, `build.gradle.kts:33`).
- Avhengighetstre inkluderer WebFlux, Micrometer tracing, NAV token-support og logstash-encoder. Genererte kilder legges til som egne source directories (`build.gradle.kts:92`, `build.gradle.kts:54`).
- Lokal profil importerer hemmeligheter fra `.env.local.properties` og definerer alle scope- og URL-variabler for integrasjoner (`src/main/resources/application-local.yaml:7`).

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
- **Deklarativ maskering** gjennom `@Maskert` og objekt-innpakninger (f.eks. `PersonIdent`) reduserer risiko for utilsiktet logging og eksponering.
- **Delt cache-backend** via Valkey i sky og Caffeine lokalt balanserer ytelse, kost og enkel feilsøking.
- **Korrelerbarhet og audit** er bygget inn fra filteret til auditlogger, slik at alle oppslag kan spores på tvers av tjenester.
