# Om prosjektet og agenter

Dette dokumentet gir en oversikt over retningslinjer for koding og prosjektstruktur i nav-persondata-api, samt en detaljert beskrivelse av agentene som inngår i prosjektet.

<details>
  <summary>Snarveier</summary>
- [Generelle retningslinjer](#generelle-retningslinjer)
- [Sikkerhets- og konfigurasjonstips](#sikkerhets--og-konfigurasjonstips)
- [Commit- og pull request-retningslinjer](#commit--og-pull-request-retningslinjer)
- [Kodestruktur og organisering](#kodestruktur-og-organisering)
- [Retningslinjer og struktur](#retningslinjer-og-struktur)
- [Agenter i prosjektet](#agenter-i-prosjektet)

</details>

## Generelle retningslinjer
- Skriv all kode med tanke på lesbarhet og vedlikeholdbarhet.
- Koden skrives primært på norsk med æøå, bruk engelske navn kun der det er naturlig (f.eks. tekniske termer).
- All kode skal være godt testet.
- Følg sikkerhetspraksis ved utvikling av ny funksjonalitet.
- Unngå å legge til unødvendige eksterne avhengigheter.
- Refaktorer kode for å forbedre lesbarhet og gjenbruk der det er mulig.
- Bruk objekt-wrapper mønster der det er hensiktsmessig.
- Ved tvil om implementasjonsvalg, søk råd for å sikre konsistens
- Oppdater relevant dokumentasjon ved større endringer i arkitektur eller integrasjoner.

## Sikkerhets- og konfigurasjonstips
- Ikke sjekk inn `.env.local.properties` eller andre filer som inneholder secrets.
- Sørg for at `.gitignore` fanger opp lokale konfigurasjonsfiler.
- Etter endringer i GCP-roller, roter eller hent secrets på nytt.
- Før du kjører lokale integrasjoner, sørg for at `gcloud auth login` er oppdatert.

## Commit- og pull request-retningslinjer
- Skriv korte, beskrivende commit-meldinger i imperativ form på norsk.
- Referer til relevante Aha!(Team Holmes), Jira(Fagsaker meldt inn av nav-brukere)- eller GitHub-issues( Meldt inn av utviklere/nais-teamet) i commit-meldinger når det er relevant.
- Unngå å blande urelaterte endringer i samme commit, spesielt når det gjelder genererte filer.
- Pull requests bør inneholde en klar beskrivelse av endringene, inkludert nye endepunkter eller skjemaoppdateringer.

## Kodestruktur og organisering
- Primær Kotlin-kode ligger i `src/main/kotlin/no/nav/persondataapi`, med pakker som reflekterer funksjonsområder.
- API-kontrakter finnes i `src/main/resources/openapi` og GraphQL-spørringer i `src/main/resources/graphql`; genererte klienter plasseres i `build/generated/...` og skal ikke redigeres manuelt.
- Tester ligger i `src/test/kotlin`, med testdata under `src/test/resources/testrespons`.
- Konfigurasjon og profilspesifikke egenskaper finnes i `src/main/resources`; lokale secrets lagres i `.env.local.properties` og er ignorert av git.

### Kodestil og navnekonvensjoner
- Følg Kotlin offisiell stil: 4-space indent, `UpperCamelCase` for klasser, `lowerCamelCase` for medlemmer, `SCREAMING_SNAKE_CASE` for konstanter.
- Plasser Spring-komponenter i koherente pakker (f.eks. `client`, `service`, `graphql`) for å holde avhengighetswiring tydelig.
- Hold genererte kilder under `build/generated` urørt; oppdater i stedet OpenAPI- eller GraphQL-filene og kjør Gradle på nytt.

## Retningslinjer og struktur
- Kodekonvensjoner (Kotlin/Java).
- Prosjektstruktur og viktige mapper/filer.
- Bygg- og testinstruksjoner (Gradle).

## Agenter i prosjektet
<details>
  <summary>Snarveier TODO</summary>
### Oversikt
- Hva er en agent i dette prosjektet?
- Hvorfor brukes agenter?

### Liste over agenter
| Navn | Beskrivelse | Ansvar |
|------|-------------|--------|
| Agent 1 | Kort beskrivelse | Hovedansvar |
| Agent 2 | Kort beskrivelse | Hovedansvar |

### Arkitektur og samspill
- Hvordan samhandler agentene med hverandre og andre komponenter?
- (Valgfritt: diagram)

### Konfigurasjon
- Hvordan konfigureres og deployes agentene?

### Endepunkter og grensesnitt
- Hvilke API-er eller grensesnitt eksponerer agentene?

### Overvåkning og logging
- Hvordan overvåkes agentene?
- Logging og feilhåndtering.

### Feilsøking
- Vanlige problemer og løsninger.
</details>
---
