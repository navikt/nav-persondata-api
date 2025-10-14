# nav-persondata-api
API for uthenting av persondata for Borger eller innbygger i Norge med Nav tilknytning.

## Komme i gang

### Bygging og testing
Du kan bygge og teste applikasjonen med `gradle build`, eller via `./gradlew build` om du ikke har gradle installert.

### Henting av secrets

Før du starter applikasjonen, må du hente ned noen secrets fra Kubernetes. Det kan du gjøre med å kjøre kommandoen `./get-secrets.sh`. De blir lagret i filen `src/main/resources/.env.local.properties`, som ignoreres av git.
For å få dette til å fungere må du ha `gcloud` installert, og logge på med `gcloud auth login`. 

### Lokal kjøring

Når du har hentet secrets, kan du starte appen med å starte Application.kt-filen via IntelliJ med profilen "local". Alternativt kan du starte den med `./gradlew bootRun --args='--spring.profiles.active=local'`

## Deployment

Applikasjonen deployes automatisk til NAIS på GCP via GitHub Actions.

For deployment til dev-miljøet, kan du kjøre actionen [Deploy til dev](https://github.com/navikt/nav-persondata-api/actions/workflows/deploy-to-dev.yml) med den branchen du ønsker å deploye. `main`-branchen deployes også til dev hver gang man merger en pull request til `main`.

For deployment til produksjon, lag en [ny release](https://github.com/navikt/nav-persondata-api/releases/new).

---

## Kode delvis generert med kunstig intelligens

Dette repoet inneholder kode som er generert med kunstig intelligens. All kode blir gjennomgått av teamet før det merges.

## Henvendelser

Spørsmål knyttet til koden eller repositoryet kan stilles som issues her på GitHub

### For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen [#team-holmes](https://nav-it.slack.com/archives/C08CZLL2QKE)
