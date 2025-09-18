# nav-persondata-api
API for uthenting av persondata for Borger eller innbygger i Norge med Nav tilknytning.

## Komme i gang
Bruker gradle wrapper, så bare klon og kjør `./gradlew build`

## Deployment

Applikasjonen deployes automatisk til NAIS på GCP via GitHub Actions.

For deployment til dev-miljøet, kan du kjøre actionen [Deploy til dev](https://github.com/navikt/nav-persondata-api/actions/workflows/deploy-to-dev.yml) med den branchen du ønsker å deploye. `main`-branchen deployes også til dev hver gang man merger en pull request til `main`.

For deployment til produksjon, lag en [ny release](https://github.com/navikt/nav-persondata-api/releases/new).

---

## Henvendelser

Spørsmål knyttet til koden eller repositoryet kan stilles som issues her på GitHub

### For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen [#team-holmes](https://nav-it.slack.com/archives/C08CZLL2QKE)
