# App-To-dos

- [x] Schreibgeschützter Dreeve-Wartungsprototyp unter Einstellungen → Wartung: Aufgaben, Räder, Intervalle und Fälligkeit abrufen. [Umfang](docs/dreeve-maintenance.md).
- [x] Wartungsdaten automatisch bei Appstart/Öffnen/nach bestätigter Fahrtverarbeitung aktualisieren, mit kontogebundenem Offline-Stand/Zeitstempel und dauerhafter Aktualisierungsvormerkung. Dreeve bleibt einzige Verwaltung. Details docs/dreeve-maintenance.md.
- [x] Dreeve-Import: bevorzugter Original-FIT-Download mit GPX-Ersatzweg, bestehenden Identitäten/Verknüpfungen und Dublettenschutz. Details docs/dreeve-original-files-api.md.
- [ ] Wartungsanbindung später um ausdrücklich beauftragte Bearbeitung/Erledigtmeldungen erweitern.
- [ ] Trainer einbauen (Umfang vor Umsetzung konkretisieren; bestehende Indoor-/FTMS-Anbindung berücksichtigen).
- [ ] Statistikfunktionen für Monat und Jahr ergänzen, um eine schnelle Übersicht über die Fahrten zu ermöglichen.

## Sensoren und Indoor-Trainer – spätere Erweiterungen

Vorgemerkt am 23.10.2026, noch kein Umsetzungsauftrag. Protokolle gemeinsam unterstützen, statt für jedes Fabrikat eine eigene Anbindung zu bauen. Details und Abgrenzung: [Quellenplanung](docs/recording-source-selection-plan.md#spätere-sensor-erweiterungen).

- [x] Bluetooth Cycling Power Service für ein separates Powermeter/Leistungspedale je Profil: unabhängige Leistungs-/Kadenzquelle mit automatischem Ersatz, Aufzeichnung und Sicherung. [Umfang und Grenzen](docs/cycling-power.md). Hardwaretests und herstellerspezifische Besonderheiten weiter offen.
- [ ] Bluetooth Cycling Speed and Cadence Service für separate Geschwindigkeits-/Kadenzsensoren anbinden; Geschwindigkeit/Distanz aus Radumdrehungen und Profil-Radumfang berechnen. Ebenfalls als nächste Ergänzung sinnvoll.
- [ ] Wahoo KICKR CORE und CORE 2 mit konkreter Firmware gegen die bestehende FTMS-Messwertanbindung prüfen; Unterstützung nicht ohne Gerätetest zusagen.
- [ ] FTMS-Trainersteuerung separat ergänzen: ERG/Zielwatt, Widerstand und Steigung, soweit vom Gerät unterstützt. Messwertempfang ist bereits vorhanden und bedeutet noch keine Steuerbarkeit.
- [ ] Zwift Ride und Zwift Click für virtuelle Schaltung vorbereiten: gemeinsames Rollentrainer-Profil für Fahrrad und Zwift-Ride-Rahmen beibehalten. Controller-Protokoll/Generation und tatsächliche Nutzbarkeit getrennt vom FTMS-Messwertempfang prüfen; keine Kompatibilität mit Zwift-Controllern allein aus FTMS ableiten. Spätere Schaltlogik an dieselbe Trainersteuerung anbinden, keine zweite Aufzeichnung. Grafik/Bedienung ausdrücklich noch offen; keine Platzhalter-UI oder funktionslosen Sensorrollen anlegen.
- [ ] Shimano Di2 prüfen: Gangposition und Schaltungsakku; Generation/Funkmodul und tatsächlich zugängliches Protokoll vor Umsetzung klären. Bluetooth-Verfügbarkeit allein garantiert keine Gangdaten.
- [ ] ANT+ bei konkretem Bedarf prüfen: eigener Protokollzugang und unterstütztes Smartphone bzw. passender Adapter erforderlich; nicht durch Bluetooth abgedeckt.

- [x] SRAM AXS als Schaltgruppe beim E-Bike: mehrere Komponenten zuordnen und Standard-Bluetooth-Akkustand abfragen; Profile/Sicherung integriert. Details docs/sram-axs.md.
- [ ] SRAM-AXS-Gerätetest am Apex-XPLR-Schaltwerk und vorhandenen Apex-Controllern; tatsächlich erreichbare Wireless Blips prüfen. Gangdaten/verschlüsseltes Protokoll und passive Akku-Advertising-Auswertung erst bei Bedarf ergänzen.
