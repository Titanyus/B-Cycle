# App-To-dos

- [ ] Wartungsanbindung zwischen Dreeve und B-Cycle.
- [ ] Trainer einbauen (Umfang vor Umsetzung konkretisieren; bestehende Indoor-/FTMS-Anbindung berücksichtigen).
- [ ] Statistikfunktionen für Monat und Jahr ergänzen, um eine schnelle Übersicht über die Fahrten zu ermöglichen.

## Sensoren und Indoor-Trainer – spätere Erweiterungen

Vorgemerkt am 23.10.2026, noch kein Umsetzungsauftrag. Protokolle gemeinsam unterstützen, statt für jedes Fabrikat eine eigene Anbindung zu bauen. Details und Abgrenzung: [Quellenplanung](docs/recording-source-selection-plan.md#spätere-sensor-erweiterungen).

- [ ] Bluetooth Cycling Power Service für separate Powermeter und Leistungspedale anbinden; danach als zusätzliche Leistungsquelle anbieten. Sinnvolle nächste Ergänzung.
- [ ] Bluetooth Cycling Speed and Cadence Service für separate Geschwindigkeits-/Kadenzsensoren anbinden; Geschwindigkeit/Distanz aus Radumdrehungen und Profil-Radumfang berechnen. Ebenfalls als nächste Ergänzung sinnvoll.
- [ ] Wahoo KICKR CORE mit konkreter Firmware gegen die bestehende FTMS-Messwertanbindung prüfen; Unterstützung nicht ohne Gerätetest zusagen.
- [ ] FTMS-Trainersteuerung separat ergänzen: ERG/Zielwatt, Widerstand und Steigung, soweit vom Gerät unterstützt. Messwertempfang ist bereits vorhanden und bedeutet noch keine Steuerbarkeit.
- [ ] Shimano Di2 prüfen: Gangposition und Schaltungsakku; Generation/Funkmodul und tatsächlich zugängliches Protokoll vor Umsetzung klären. Bluetooth-Verfügbarkeit allein garantiert keine Gangdaten.
- [ ] ANT+ bei konkretem Bedarf prüfen: eigener Protokollzugang und unterstütztes Smartphone bzw. passender Adapter erforderlich; nicht durch Bluetooth abgedeckt.
