
### Innlogging og autentisering og henting av data for visning i Watson søk
Denne tegningen gir en overordnet oversikt over hvordan innlogging, autentisering og 
henting av persondata skjer i nav-persondata-api for visning i Watson søk.
Sekvensene er numereret med interaksjoner tiknyttet steget.
1. Saksbehandler logger inn i Watson søk, som henter token fra AAD.
2. sjekk av saksbehandlers tilgang til persondata-api:
  2.1 nav-persondata-api mottar forespørsel med token
  2.2 nav-persondata-api sender personident og token til nav-persondata-api
  2.3 nav-persondata-api sjekker om saksbehandler har tilgang til å hente data for personidenten
  2.4 nav-persondata-api returnerer tilgangssvar (403 Forbidden ved avslag)
3. Henting av informasjon tiknyttet personident:
  3.1 nav-persondata-api mottar forespørsel om henting av persondata for personident
  3.2 nav-persondata-api sjekker saksbehandlers tilgang til persondata for personident
  3.3 nav-persondata-api logger avslag/godkjenning av tilgang
  3.4 nav-persondata-api sjekker med tilgangsmaskinen om saksbehandler har tilgang til personident
  3.5 nav-persondata-api returnerer tilgangssvar (403 Forbidden ved avslag)
4. Henting av persondata ved godkjent tilgang:
  4.1 nav-persondata-api henter vedtaksdata fra ulike kilder:
    4.2 Hent utbetalinger fra Utbetaling
    4.3 Hent ytelser fra Ytelser
    4.4 Hent arbeidsforhold fra Arbeidsforhold
    4.5 Hent meldekortdata fra DAG, AAP, TIL
    4.6 nav-persondata-api returnerer data eller feil til Watson søk

Det vil tilkomme fler datakilder for saksbehandler i fremtiden.
````mermaid
flowchart TD
    A[Watson søk] -->|1. Logg inn saksbehandler| D[Hent fra AAD]
    A -->|2.2 Forespørsel personident| B[nav-persondata-api]
    B -->|5.1 Logg nødvendige feilmeldinger| M{Applikasjonslogging}
    M -->|5.2 Logg sensitiv data|N[Secure logg]
    M -->|5.3 Logg generell info|O[Applikasjonslogg]
    B -->|5.4 Feilmeldding|A
    B -->|2.3 Sjekk om saksbehandler har tilgang| C{Sjekk tilganger}
    C -->| Logg tilgangsforespørsel| G[Audit logg]
    B -->|2.4 tilgangssvar| A[Returner 403 Forbidden]
    B -->|3.1 Hent persondata| C{Sjekk tilganger}
    C -->|3.2 Logg tilgangsforespørsel| G[Audit logg]
    C -->|3.3 Logg avslag/godkjenning| G[Audit logg]
    C -->|3.4 Sjekk tilgang til personident| E[Tilgangsmaskinen]
    C --->|3.5 Returner Tilgangssvar|B
    B -->|4.1 Hent data|F{Hent vedtaksdata}
    F --->|4.2 Hent utbetalinger| I[Utbetaling]
    F --->|4.3 Hent ytelser| J[Ytelser]
    F --->|4.4 Hent arbeidsforhold| K[Arbeidsforhold]
    F --->|4.5 Hent Meldekort| L[DAG, AAP, TIL]
    B -->|4.6 Returner data eller feil| A
    
````

    
    
    
    



