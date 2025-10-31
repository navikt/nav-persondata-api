# Arkitektur

## Oversikt
- Spring Boot-applikasjon skrevet i Kotlin med caching aktivert globalt (`src/main/kotlin/no/nav/persondataapi/Application.kt:7`). Cache-manager konfigureres med Caffeine og styres via `application.yaml` (`src/main/kotlin/no/nav/persondataapi/konfigurasjon/CacheConfiguration.kt:16`).
- REST-API-laget tilbyr beskyttede POST-endepunkter for personopplysninger, arbeidsforhold, inntekt og stønader. Controllerne blokkerer inn i koroutines for å kalle suspenderende tjenester (`src/main/kotlin/no/nav/persondataapi/rest/oppslag/PersonopplysningerController.kt:15`, `ArbeidsforholdController.kt:15`, `InntektController.kt:15`, `StønadController.kt:14`).
- Tjenestene aggregerer, beriker og maskerer data før svar returneres (`src/main/kotlin/no/nav/persondataapi/service/PersonopplysningerService.kt:26`, `ArbeidsforholdService.kt:21`, `InntektService.kt:21`, `StønadService.kt:17`).
- Domeneobjekter i `rest/domene` markerer sensitive felter med `@Maskert`, mens `MaskeringUtil` traverserer objekter rekursivt og erstatter verdier ved behov (`src/main/kotlin/no/nav/persondataapi/rest/domene/PersonInformasjon.kt:5`, `src/main/kotlin/no/nav/persondataapi/rest/oppslag/Maskert.kt:21`, `MaskeringUtil.kt:15`).

## Integrasjoner og dataflyt
- WebClient-instansene konfigureres sentralt med Call-ID-filter, Micrometer-merking og systemspesifikke observation-konvensjoner (`src/main/kotlin/no/nav/persondataapi/konfigurasjon/WebClientConfig.kt:42`, `ClientMetricsConfig.kt:19`).
- `TokenService` håndterer både OBO-veksling og client-credentials mot NAVs sikkerhetsplattform basert på miljøvariabler (`src/main/kotlin/no/nav/persondataapi/service/TokenService.kt:19`).
- PDL-data hentes via GraphQL med caching og tilpassede headers (`src/main/kotlin/no/nav/persondataapi/integrasjon/pdl/client/PdlClient.kt:22`); kodeverk- og organisasjonsdata hentes og brukes til beriking (`src/main/kotlin/no/nav/persondataapi/service/KodeverkService.kt:4`, `integrasjon/kodeverk/client/KodeverkClient.kt:27`, `integrasjon/ereg/client/EregClient.kt:16`).
- Inntekt, arbeidsforhold og utbetalinger konsumeres via egne klienter og mappes til interne domeneobjekter (`src/main/kotlin/no/nav/persondataapi/integrasjon/inntekt/client/InntektClient.kt:28`, `integrasjon/aareg/client/AaregClient.kt:28`, `integrasjon/utbetaling/client/UtbetalingClient.kt:23`).
- Tilgangskontroll mot Tilgangsmaskinen evalueres og brukes til å bestemme maskering, kombinert med lokale AD-grupper (`src/main/kotlin/no/nav/persondataapi/integrasjon/tilgangsmaskin/client/TilgangsmaskinClient.kt:25`, `service/TilgangService.kt:21`, `domene/Grupper.kt:14`).

## Sikkerhet og observabilitet
- JWT-validering er aktivert gjennom `SecurityConfiguration`, med issuer og audience konfigurert i `application.yaml` (og overrides i `application-local.yaml`) (`src/main/kotlin/no/nav/persondataapi/konfigurasjon/SecurityConfiguration.kt:7`, `src/main/resources/application.yaml:49`).
- `BrukertilgangService` kombinerer token-claims, Tilgangsmaskin-respons og AD-grupper for å avgjøre tilgangsstatus (`src/main/kotlin/no/nav/persondataapi/service/BrukertilgangService.kt:12`, `TilgangService.kt:21`).
- Alle oppslag auditeres via `AuditService`, og `NavCallIdServletFilter` sørger for korrelasjons-ID i MDC og utgående kall (`src/main/kotlin/no/nav/persondataapi/service/AuditService.kt:21`, `konfigurasjon/CallIdConfig.kt:20`).
- Aktuatorendepunkter og Prometheus-metrikker eksponeres via `application.yaml`, og WebClient-observasjoner tagges per nedstrømsystem (`src/main/resources/application.yaml:32`, `src/main/kotlin/no/nav/persondataapi/konfigurasjon/ClientMetricsConfig.kt:26`).

## Bygg og konfig
- Bygges med Gradle og Spring Boot 3.2 / Kotlin 1.9. GraphQL- og OpenAPI-klienter genereres som del av kompilasjonsløpet (`build.gradle.kts:6`, `build.gradle.kts:16`, `build.gradle.kts:33`).
- Avhengighetstre inkluderer WebFlux, Micrometer tracing, NAV token-support og logstash-encoder. Genererte kilder legges til som egne source directories (`build.gradle.kts:92`, `build.gradle.kts:54`).
- Lokal profil importerer hemmeligheter fra `.env.local.properties` og definerer alle scope- og URL-variabler for integrasjoner (`src/main/resources/application-local.yaml:7`).

## Naturlige neste steg
1. Kjør `./gradlew build` for å verifisere at generatorene og byggløpet fungerer i miljøet ditt.
2. Vurder å supplere dokumentasjonen med kontekst- eller sekvensdiagram dersom arkitekturen skal presenteres for nye teammedlemmer.
