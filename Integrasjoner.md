
### Innlogging og autentisering i Watson søk
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
    C -->|3.3 Logg avslag/godkjenning| H[Audit logg]
    C -->|3.4 Sjekk tilgang til personident| E[Tilgangsmaskinen]
    C --->|3.5 Returner Tilgangssvar|B
    B -->|4.1 Hent data|F{Hent vedtaksdata}
    F --->|4.2 Hent utbetalinger| I[Utbetaling]
    F --->|4.3 Hent ytelser| J[Ytelser]
    F --->|4.4 Hent arbeidsforhold| K[Arbeidsforhold]
    F --->|4.5 Hent Meldekort| L[DAG, AAP, TIL]
    B -->|4.6 Returner data eller feil| A
    
````

    
    
    
    



