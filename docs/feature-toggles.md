# Feature toggles

Feature toggles (feature flags) brukes i Holmes-porteføljen for å skille deploy fra release.
Ny funksjonalitet med risiko for å gå skeis sendes ut bak en toggle, slik at den kan aktiveres
kontrollert etter prodsetting — uten ny deployment.

Unleash-dashboardet finner du her:
[holmes-unleash-web.iap.nav.cloud.nais.io](https://holmes-unleash-web.iap.nav.cloud.nais.io/projects/default?limit=25&favoritesFirst=true&sortBy=createdAt&sortOrder=desc)

---

## Livssyklus

```
1. Opprett toggle i Unleash   →   2. Implementer i koden   →   3. Deploy
        ↓
4. Aktiver i dev og verifiser   →   5. Aktiver i prod og verifiser
        ↓
6. Slett toggle fra kode og Unleash
```

### 1. Opprett toggle i Unleash

Gå til dashboardet og opprett en ny toggle med riktig navn (se navnekonvensjoner under).
Velg type **Release** for funksjonalitet som skal rulles ut og deretter fjernes.

### 2. Implementer i koden

Legg til toggle-verdien i `Toggle`-enumen i `nav-persondata-api`:

```kotlin
// src/main/kotlin/no/nav/persondataapi/unleash/Toggle.kt
enum class Toggle(val toggleName: String) {
    WATSON_SOK_V_1_2("watson-sok-v-1-2"),
}
```

Bruk `FeatureToggleService` der funksjonaliteten skal gates:

```kotlin
@Service
class MinService(private val toggles: FeatureToggleService) {
    fun gjørNoe() {
        if (toggles.isEnabled(Toggle.WATSON_SOK_V_1_2)) {
            // ny kode
        } else {
            // eksisterende kode / fallback
        }
    }
}
```

Tilsvarende toggle implementeres i `watson-søk` (frontend) om den deles mellom applikasjonene.

### 3. Deploy

Deploy til dev og prod som normalt. Funksjonaliteten er skrudd av til togglen aktiveres.

### 4. Aktiver i dev og verifiser

Skru på togglen i **development**-miljøet i Unleash-dashboardet og verifiser at funksjonaliteten
fungerer som forventet. Togglen skal alltid være verifisert i dev **før** den aktiveres i prod.

### 5. Aktiver i prod og verifiser

Skru på togglen i **production**-miljøet. Det er **utvikleren som implementerte featuren** som
har ansvar for å aktivere og verifisere.

### 6. Rydde opp

Når featuren er verifisert i prod og anses som stabil:

1. Fjern toggle-verdien fra `Toggle`-enumen
2. Fjern alle `isEnabled`-sjekker fra koden (behold kun den nye kodeveien)
3. Slett togglen fra Unleash-dashboardet

Ikke la "døde" toggles bli liggende — de er teknisk gjeld.

---

## Navnekonvensjoner

Format: `<prefix>-v<major>-<minor>` (løs semver-inspirert)

| Kontekst | Prefix | Eksempel |
|----------|--------|---------|
| Funksjonalitet i watson-søk | `watson-sok` | `watson-sok-v-1-2` |
| Funksjonalitet i watson-sak | `watson-sak` | `watson-sak-v-2-0` |
| Generell / tverrgående | _(fritt valg med begrunnelse)_ | `ny-tilgangspolicy` |

Versjonsnummeret speiler typisk hvilken release funksjonaliteten tilhører.
Avvik fra standarden er tillatt, men krev en god grunn.

---

## Lokalt utviklingsmiljø

Lokalt (uten `UNLEASH_SERVER_API_URL` satt) returnerer alle toggles `false` automatisk.
Du kan overstyre dette ved å kalle `FakeUnleash.enable(...)` i tester, eller ved å sette
env-variablene manuelt via `.env.local.properties`.
