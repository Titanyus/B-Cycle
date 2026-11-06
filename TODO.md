# App-To-dos

- [x] Schreibgeschützter Dreeve-Wartungsprototyp unter Einstellungen → Wartung: Aufgaben, Räder, Intervalle und Fälligkeit abrufen. [Umfang](docs/dreeve-maintenance.md).
- [x] Wartungsdaten automatisch bei Appstart/Öffnen/nach bestätigter Fahrtverarbeitung aktualisieren, mit kontogebundenem Offline-Stand/Zeitstempel und dauerhafter Aktualisierungsvormerkung. Dreeve bleibt einzige Verwaltung. Details docs/dreeve-maintenance.md.
- [x] Dreeve-Import: bevorzugter Original-FIT-Download mit GPX-Ersatzweg, bestehenden Identitäten/Verknüpfungen und Dublettenschutz. Details docs/dreeve-original-files-api.md.
- [ ] Wartungsanbindung später um ausdrücklich beauftragte Bearbeitung/Erledigtmeldungen erweitern.
- [ ] Trainer einbauen (Umfang vor Umsetzung konkretisieren; bestehende Indoor-/FTMS-Anbindung berücksichtigen).
- [x] Statistikseite: monatliche Kilometer mit Jahresvergleich, ISO-Wochen des aktuellen Jahres und Fahrzeit-Tortengrafik je Fahrrad in Profilfarben. Cached Bibliotheksauswertung; siehe [Statistiken](docs/ride-statistics.md). Im Quellstand nach Build95, noch nicht als APK veröffentlicht.

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

- [x] intervals.icu-Sammelupload in Daten & Speicher: vorhandene Fahrten prüfen und nur fehlende übertragen, einschließlich bestätigter Dreeve-Importe mit Messdaten. Details docs/intervals-bulk-upload.md.
- [x] Dreeve-Haken bei als Gefahren importierten FIT-Originalen: vorhandene Kontoverknüpfung auch ohne eigenen Uploadbeleg anzeigen.
- [x] SRAM-Suche: spätere Namensantworten und priorisierte Kandidaten berücksichtigen, Probe-Timeout verhindert Auswahl nicht. Hardwaretest am Apex bleibt offen.

## Automatische Intervallerkennung aus gefahrenen Trainings

Vorgemerkt am06.11.2026 auf Nutzerwunsch, noch kein Umsetzungsauftrag. Inspiration: [Dreeve #2973 – Automatic workout & interval detection](https://github.com/dreeveapp/dreeve/issues/2973), [FIT Insight – Beispielprojekt](https://github.com/effectpears/fit-training-insight) und [Demo](https://effectpears.github.io/fit-training-insight/).

- [ ] Absolvierte Indoor-/Virtual-Ride-Fahrten anhand ihrer aufgezeichneten Leistung automatisch in Aufwärmen, Belastungen, Erholung, wiederholte Intervallsätze und Ausrollen gliedern. Beispiele:60/60,40/20, längere Schwellenblöcke, mehrstufige Wiederholungen. Kompakte Satzübersicht plus Markierungen im Leistungsdiagramm; bei Fahrten ohne GPS könnte die Intervallübersicht den Kartenbereich ersetzen. Oberfläche vor Umsetzung festlegen.
- [ ] Vorhandene Workout-/Lap-Marker bevorzugen, sonst erklärbare Mustererkennung aus geglätteter Analysekurve (Schwelle/Hysterese, Mindestdauern, Kontrast und Satzgruppierung). Gleichmäßige Fahrten/Rampen nicht als künstliche Intervalle erkennen; unsichere Ergebnisse kenntlich machen. Anfangs auf Indoor/Virtual Ride begrenzen.
- [ ] Rohdaten und FIT-Aufzeichnung unverändert lassen: Glättung nur zur Erkennung/Darstellung, Kennzahlen der erkannten Abschnitte aus Originaldaten. Sensorlücken nicht als echte0-W-Erholung werten. Lokale Analyse auf bestehendem Analyseworker/cache, kein zweiter Aufnahmeweg. Beispielprojekt insbesondere bezüglich Lap-Priorisierung, mehrstufiger Sätze und Darstellung prüfen; Lizenz vor eventueller Codeübernahme klären. Abgrenzung: nachträgliche Erkennung gefahrener Intervalle, unabhängig von unseren importierten Trainingsvorlagen und deren Q+/Q/S/E/E+-Varianten.
