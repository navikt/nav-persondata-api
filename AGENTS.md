# Repository Guidelines
## Project Structure & Module Organization
- Primary Kotlin sources live in `src/main/kotlin/no/nav/persondataapi`, with packages mirroring feature areas.
- API contracts reside in `src/main/resources/openapi` and GraphQL queries in `src/main/resources/graphql`; generated clients land in `build/generated/...` and should not be hand-edited.
- Tests sit in `src/test/kotlin`, with fixtures under `src/test/resources/testrespons`.
- Configuration and profile-specific props go in `src/main/resources`; local secrets are written to `.env.local.properties` by tooling and stay git-ignored.

## Build, Test & Development Commands
- `./get-secrets.sh` fetches NAIS credentials into the local env file; run once per session when targeting secure backends.
- `./gradlew clean build` compiles sources, runs generators (`openApiGenerate`, `graphqlGenerateClient`) and executes the full test suite.
- `./gradlew test` is the fastest loop for unit and integration tests.
- `./gradlew bootRun --args='--spring.profiles.active=local'` starts the Spring Boot app locally with the `local` profile.

## Coding Style & Naming Conventions
- Follow Kotlin official style: 4-space indent, `UpperCamelCase` for classes, `lowerCamelCase` for members, `SCREAMING_SNAKE_CASE` for constants.
- Place Spring components within coherent packages (e.g., `client`, `service`, `graphql`) to keep dependency wiring obvious.
- Keep generated sources under `build/generated` untouched; instead update the OpenAPI or GraphQL files and rerun Gradle.

## Testing Guidelines
- Use JUnit 5 with MockK/Kotlin test helpers; name files `SomethingTest.kt` and annotate Spring slices only when needed.
- Prefer pure Kotlin tests for cache and client logic; leverage `src/test/resources/testrespons` for canned payloads.
- Ensure new tests run via `./gradlew test` before pushing; aim to cover error paths, especially for caching and external integrations.

## Commit & Pull Request Guidelines
- Commits in history are short, descriptive sentences (often Norwegian imperatives); mirror that style and keep scope focused.
- Reference Jira or GitHub issues in the footer when relevant, and avoid mixing unrelated changes with generated diffs.
- Pull requests should describe the change, call out new endpoints or schema updates, list manual verifications, and include screenshots or log snippets when API responses change.

## Security & Configuration Tips
- Never commit `.env.local.properties` or secrets copied from NAIS; verify `.gitignore` catches local files.
- Rotate or re-fetch secrets after GCP role changes, and check that `gcloud auth login` is recent before running local integrations.
