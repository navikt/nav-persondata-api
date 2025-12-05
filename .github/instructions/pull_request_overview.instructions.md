# Pull Request Overview Instruction

Skriv alltid oversikten over pull requesten på norsk.

- Bruk et profesjonelt, men uformelt språk.
- Start med en kort oppsummering av hva som er endret.
- Beskriv deretter hvorfor endringene ble gjort.
- Om det er tekniske detaljer (for eksempel migrasjoner, konfigurasjonsendringer eller API-oppdateringer), forklar kort hva de gjør.
- Avslutt gjerne med en punktliste over hva som bør testes eller verifiseres.

Eksempel:
> Denne PR-en legger til støtte for nye MQTT-meldinger i sensormodulen.  
> Endringen inkluderer oppdatering av `MqttListenerService` og tilpasninger i `SensorRepository`.  
> Dette ble gjort for å forbedre ytelsen og redusere latency.
>
> **Tester:**
> - Sjekk at MQTT-meldinger fortsatt prosesseres riktig
> - Verifiser at loggene vises i Grafana
