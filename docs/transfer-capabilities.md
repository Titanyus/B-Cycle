# Angaben zu Import und Export

Unter Daten & Speicher zeigen die Details der Import-/Exportwege zwei kurze Angaben:

- **→ IN:** Was BICYCLE über diesen Weg empfängt oder importiert.
- **← OUT:** Was BICYCLE über diesen Weg exportiert oder überträgt.

Die Angaben stehen bei Streckenimport, Lokal, WebDAV, intervals.icu, Dreeve, Strava
und Fahrten sichern direkt im bestehenden Accordion. Die vorhandenen Controls,
Schalter, Aktivierungsbedingungen und Übertragungen bleiben erhalten.

Stringressourcen: `app/src/main/res/values/transfer_capabilities.xml`.
Zuordnung und Darstellung: `DataSettingsView`.

## Verbindliche Pflege bei Änderungen

Bei jeder Anpassung, Erweiterung oder Ergänzung eines Weges gehören folgende Punkte
zur selben Implementierungsänderung:

1. IN-/OUT-Texte mit dem tatsächlich implementierten Verhalten abgleichen. Neue Wege
   erhalten beide Angaben; fehlende Richtungen und Grenzen ausdrücklich nennen.
2. Zugehörige Import-/Exporteinstellungen, Validierung, verfügbare Optionen,
   Aktivierungsbedingungen und Hilfetexte aktualisieren.
3. Neue/geänderte gespeicherte öffentliche Einstellungen in Export und Import der
   Einstellungssicherung berücksichtigen, einschließlich Profilumfang, Standardwerten
   und Migration. Keine geheimen Zugangsdaten oder Android-Berechtigungen exportieren.
4. Dokumentation und relevante Prüfungen entsprechend anpassen.

Dabei Dateiformate, verfügbare Messwerte/Metadaten, Herkunftsgrenzen und den Unterschied
zwischen normalem FIT-Export, Dienstupload und vollständiger Sicherung erhalten.
Nur tatsächlich vorhandene Funktionen beschreiben. Die Pflege ist eine feste
Entwicklungsregel in AGENTS.md; die Beschreibung wird nicht aus API-Fähigkeiten
abgeleitet, die BICYCLE noch nicht umgesetzt hat.
