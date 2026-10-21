# B|CYCLE 0.8.20

Abschluss der fünf geplanten Schritte zur Entkopplung von Fahrtabschluss, Aufzeichnungsfeldern und Dashboard. Diese Version ergänzt Schritt 4/5 auf Basis von 0.8.19.

- Fachlicher Aufzeichnungsfeldkatalog unabhängig von der Dashboardauswahl. Journal, FIT, Indoor und Einstellungssicherungen verwenden stabile IDs, Einheiten und Scope direkt aus dem Katalog; UI-Beschriftungen und Gruppen bleiben getrennt.
- Bestehende Felddefinitionen einschließlich Altdefinitionen, Reihenfolge und Standardauswahl erhalten. Keine Migration von Aufzeichnungen oder Profilen.
- Reichweite wird numerisch gemeinsam für Aufzeichnung und Anzeige ermittelt. Keine Rückumwandlung von Anzeigetexten in Messwerte.
- Bisherige Live-Messwertformatierung in eine eigene Präsentationsdatei ausgelagert. Bedienung, Farben, Texte, Einheiten, Rundung und Fallbacks bleiben erhalten.
- Keine neuen externen Abhängigkeiten oder Änderungen an BLE, Quellenwahl, Frischegrenzen, Aufnahmetakten oder Leistungsberechnung.

105 gezielte Testausführungen erfolgreich, darunter bytegleiche Wiedererzeugung eines alten synthetischen Journals, Vergleich von FIT-Messwerten/Zusammenfassungen/Developer-Felddefinitionen mit Referenz, Messwertverarbeitung ohne Android/Dashboard, UI/Indoor, Backups, Profile, Fahrtnamen und Aufzeichnung auf SDK 33/35/36. Kein vollständiger Gesamttestlauf; physische Sensor-/Telefonprüfung bleibt ergänzend erforderlich.

[Aufzeichnungsfelder und Messwerte](recording-fields.md) · [Speicherergebnisse](recording-results.md) · [Übergabe](CONTINUE-HERE.md).
