# Aktueller Nachtrag30.10.2026

Nach Build105: kompakte Cloud-Angaben in backup_domains.xml/intervals_hrv.xml. Dreeve steht vor intervals.icu; Dreeve IN „FIT-Streckendaten | Wartungsintervalle“, intervals OUT „FIT-Streckendaten | HRV-Daten“. Neuer täglicher automatischer HRV-Upload und expliziter Nur-fehlende-Tage-Sammelupload: [intervals-hrv.md](intervals-hrv.md). Bisherige Aussagen „kein HRV-Cloud-Upload“ sind damit für intervals.icu überholt. Messungs-/Tagesbefinden-Sicherung und täglicher Wellness-Upload bleiben getrennte Wege. Bestehende Konto-/WLAN-Einstellungen samt verschlüsselter Zugangssicherung gelten unverändert, keine neuen öffentlichen Einstellungen.

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
   und Migration. Zugangsdaten ausschließlich im ausdrücklich passwortverschlüsselten AES-256-ZIP sichern; öffentliches altes JSON bleibt geheimnisfrei. Android-Berechtigungen nicht exportieren. Siehe [Einstellungssicherung](settings-archive.md).
4. Dokumentation und relevante Prüfungen entsprechend anpassen.

Dabei Dateiformate, verfügbare Messwerte/Metadaten, Herkunftsgrenzen und den Unterschied
zwischen normalem FIT-Export, Dienstupload und vollständiger Sicherung erhalten.
Nur tatsächlich vorhandene Funktionen beschreiben. Die Pflege ist eine feste
Entwicklungsregel in AGENTS.md; die Beschreibung wird nicht aus API-Fähigkeiten
abgeleitet, die BICYCLE noch nicht umgesetzt hat.

HRV-Pulsdaten ist ein eigener Sticky-Abschnitt mit direktem Exportbutton. Seine IN-/OUT-Grenzen stehen in der zugehörigen Daten-Hilfe (`hrv_export_help`): vollständige Wiederherstellung über BICYCLE-Sicherung; gezielter manueller Analyseexport als ZIP mit einer JSON je Messung inklusive Rohdaten und Ablehnungen. Keine Cloud-Synchronisierung und kein Import fremder HRV-Analyseformate.

Nach Build 61: Vollständige HRV-Sicherung und HRV-Analyse-ZIP enthalten optional das ausgefüllte Tagesbefinden getrennt vom unveränderten Ergebnis. Wiederherstellung legt die optionale Antwortdatei wieder an; alte Sicherungen ohne Antworten bleiben kompatibel. Reine Einstellungssicherung enthält globale Fragenauswahl/eigene Feldnamen und optionale Geschlechtsangabe, keine Antworten. Kein automatischer Upload. Sichtbare Texte in hrv.xml/transfer_capabilities.xml und Daten-Exportbeschreibung angepasst.
