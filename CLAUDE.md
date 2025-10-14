# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`nav-persondata-api` is a Kotlin/Spring Boot API for retrieving person data for citizens or residents in Norway with NAV affiliation. The application integrates with multiple Norwegian government services including PDL (Persondataløsningen), Aareg (Arbeidsgiver- og arbeidstakerregisteret), Ereg (Enhetsregisteret), Inntekt, and Utbetaling systems.

The codebase is written in Norwegian, including variable names, comments, and documentation.

## Build and Development Commands

### Building and Testing
- Build the project: `./gradlew build`
- Run tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "ClassName"`
- Run a single test method: `./gradlew test --tests "ClassName.methodName"`

### Code Generation
The project uses code generation for GraphQL and OpenAPI clients:
- GraphQL client code is generated from schemas in `src/main/resources/graphql/`
- OpenAPI client code is generated from `src/main/resources/openapi/ikomp-inntektshistorikk-api-2.1.2-swagger.json`
- Code generation tasks: `graphqlGenerateClient` and `openApiGenerate`
- Generated code is placed in `build/generated/source/graphql/main` and `build/generate-resources/main/src/main/kotlin`
- The `compileKotlin` task automatically depends on both code generation tasks

### Local Development Setup

1. **Prerequisites**: Install `gcloud` CLI and authenticate with `gcloud auth login`

2. **Fetch Secrets**: Run `./get-secrets.sh` to retrieve necessary secrets from Kubernetes (dev-gcp cluster, holmes namespace). This creates `src/main/resources/.env.local.properties` (git-ignored) with Azure credentials.

3. **Run the Application**:
   - Via IntelliJ: Start `Application.kt` with the "local" profile
   - Via command line: `./gradlew bootRun --args='--spring.profiles.active=local'`

The application runs on port 8080 by default.

## Architecture

### Package Structure

- `no.nav.persondataapi.rest.oppslag`: REST controllers for person data lookups
  - `PersonbrukerController`: User access verification
  - `PersonopplysningerController`: Personal information retrieval
  - `ArbeidsforholdController`: Employment information
  - `InntektController`: Income information
  - `StønadController`: Benefits information

- `no.nav.persondataapi.service`: Business logic layer
  - `TilgangService`: Access control logic (basic vs extended access levels)
  - `BrukertilgangService`: User access status checks
  - `TokenService`: Azure AD token management for service-to-service calls
  - `KodeverkService`: Code registry lookups

- `no.nav.persondataapi.integrasjon`: External service integrations (client layer)
  - `pdl.client`: PDL (Person Data Registry) GraphQL client
  - `aareg.client`: Aareg (Employment Registry) REST client
  - `ereg.client`: Ereg (Organization Registry) REST client
  - `inntekt.client`: Income history client
  - `utbetaling.client`: Payment/benefits client
  - `tilgangsmaskin.client`: Access control service client
  - `kodeverk.client`: Code registry client

- `no.nav.persondataapi.konfigurasjon`: Spring configuration classes
  - `SecurityConfiguration`: JWT token validation setup
  - `WebClientConfig`: WebClient configuration for external service calls
  - `ClientMetricsConfig`: Metrics configuration for HTTP clients
  - `CallIdConfig`: Call-ID/correlation-ID handling

- `no.nav.persondataapi.rest.domene`: REST API domain models
- `no.nav.persondataapi.domene`: Core domain models (e.g., `Grupper` for AD group management)

### Key Architectural Patterns

**Access Control**: The application implements a two-tier access control system:
- **Basic Access** (`0000-GA-kontroll-Oppslag-Bruker-Basic`): Standard access with certain restrictions
- **Extended Access** (`0000-GA-kontroll-Oppslag-Bruker-Utvidet`): Enhanced permissions for sensitive data

Access decisions are made by combining:
1. Azure AD group membership (checked via `TilgangService.harUtvidetTilgang()`)
2. Tilgangsmaskinen service responses (various rejection codes like `AVVIST_STRENGT_FORTROLIG_ADRESSE`)
3. Custom override logic in `TilgangService` extension functions (`harTilgangMedBasicAdgang()`, `skalMaskere()`)

**External Service Integration**: All external services use:
- WebClient for HTTP communication (Spring WebFlux client, not full WebFlux stack)
- Token exchange via NAV's token-client-spring for machine-to-machine authentication
- Structured error handling with custom result types

**GraphQL Integration**: PDL integration uses:
- Expedia GraphQL Kotlin client (`com.expediagroup:graphql-kotlin-*`)
- Schema located at `src/main/resources/graphql/schema/pdl-api-sdl.graphqls`
- Queries defined in `src/main/resources/graphql/queries/`
- Type-safe generated client code

**OpenAPI Integration**: Inntekt integration uses OpenAPI Generator to create client code from Swagger specs.

## Technology Stack

- **Language**: Kotlin 1.9.24
- **Framework**: Spring Boot 3.2.7 with Spring Web (MVC, not WebFlux)
- **JVM**: Java 21
- **Security**: NAV token-support library for JWT validation
- **HTTP Client**: Spring WebFlux WebClient (without full reactive runtime)
- **Observability**: Micrometer with OpenTelemetry tracing, Prometheus metrics, structured JSON logging (Logstash encoder)
- **Testing**: JUnit 5, Kotlin coroutines test support

## Spring Profiles

- **default**: Production configuration, expects environment variables from NAIS platform
- **local**: Local development, imports `.env.local.properties` and uses hardcoded dev URLs/endpoints

## Deployment

The application is deployed to NAIS on GCP via GitHub Actions:
- **Dev deployment**: Trigger manually via [Deploy til dev](https://github.com/navikt/nav-persondata-api/actions/workflows/deploy-to-dev.yml) or automatically on merge to `main`
- **Production deployment**: Create a [new release](https://github.com/navikt/nav-persondata-api/releases/new)
- Configuration files: `.nais/nais.yaml` (dev) and `.nais/prod.yaml` (prod)

## Important Notes

- This is a **Norwegian government project** with Norwegian naming conventions throughout
- All code, comments, and documentation are in Norwegian
- The application handles sensitive personal data and has strict access controls
- Never commit secrets or the `.env.local.properties` file
- The `get-secrets.sh` script requires access to the dev-gcp Kubernetes cluster
