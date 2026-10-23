# Vollständige Einstellungssicherung

Stand nach Build 70, 23.10.2026. Auf ausdrücklichen Nutzerwunsch werden auch
Zugangsdaten übertragen, ausschließlich mit passwortgeschützter Verschlüsselung.

## Bedienung und Format

Daten & Speicher → Einstellungen sichern: Bereiche wählen, Passwort zweimal
eingeben (12–1024 Zeichen, vorzugsweise lange Passphrase), Speicherort im Android-
Dateidialog wählen. Ausgabe `BICYCLE_Settings_<Datum>.zip`. Die Wiederherstellung
fragt zuerst das Passwort ab; danach Bereiche wählen und das Ersetzen bestätigen.
Abbrechen, falsches Passwort und beschädigte Archive wenden keine Einstellungen an.
Das Passwort wird weder gespeichert noch über Autofill oder Activity-State gesichert;
bei Verlust gibt es keine Wiederherstellung des Passworts.

Standard WinZip-AES-256-ZIP mit genau einem verschlüsselten Eintrag `settings.json`.
Enthalten sind das bisherige öffentliche JSON-Schema, explizite Zugangsdaten und
begrenzte WebP-Bilder. Zip4j 2.11.5 (Apache-2.0) ergänzt die fehlende ZIP-Verschlüsselung
von java.util.zip; Lizenz in der App unter Bibliothekslizenzen. Kompatible AES-ZIP-
Programme können das Archiv ebenfalls lesen. Keine eigene Kryptografie oder
Extraktion fremder Dateipfade. Ein-/Ausgabe maximal 32 MiB, Bilder maximal 4 MiB
und 1200 × 1200. Im Cache entsteht nur bereits verschlüsseltes ZIP, kein Klartext.

Alte öffentliche JSON-Sicherungen bleiben lesbar, enthalten aber keine Zugangsdaten
oder Bilder. Das bisherige JSON-Schema und stabile Feld-/Profilkennungen bleiben
kompatibel. `SettingsBackup.export` bleibt die geheimnisfreie Legacy-Schnittstelle;
neue UI-Sicherungen verwenden ausschließlich `SettingsArchive`.

## Geprüfter Umfang

| Bereich | Gesicherte Einstellungen |
| --- | --- |
| Design & UI | Theme-Auswahl/eigene Themes, globaler heller Einstellungsmodus, Glow-/Vollbildoptionen, Benachrichtigungsauswahl/Position/Dauer, Fahrten-Sicherung an/aus/Ziel |
| Dashboard | Seiten/Katalog/aktive Seite, Templates, Datenfelder/Kombifelder, Sichtbarkeit, Graphen/Anzeigemodi/Zeitfenster, Trainingshilfe/Zonen, Trendindikatoren, Kartenfelder, Trinkerinnerung |
| Aufzeichnung/FIT | Messfelder, Metadaten-Auswahl, Geschwindigkeits-/Distanzquelle, lokale Speicherung, Fahrtbenennung, lokaler Exportordner als URI-Referenz |
| Karten | Quelle, Nachtmodus, Folgen/Ausrichtung, Positionsanker, Track-/Routensichtbarkeit, Darstellungsstile, Höhenkorrektur, Zoom/Position, ausgewählte Track-IDs, Thunderforest-Schlüssel |
| Fahrer/Profile/HRV | Fahrerwerte einschließlich Geschlecht/Herzfrequenzmethode, sämtliche aktiven/inaktiven Fahrradprofile mit Sensorzuordnung/Korrekturfaktoren/FIT-Auswahl, UUIDs/zuletzt bekannter Radtyp, profilbezogene UI-/Karten-Snapshots, Fahrradbilder, Dreeve-Radzuordnungen, HRV-Zeiten/Tagesangebot/Endsignale, Frageauswahl/eigene Fragen |
| Cloud | WebDAV-Konfiguration/Passwort, intervals.icu-Athlet/API-Schlüssel, Dreeve-Server/Token, Strava-Client/Konto/Client-Secret/OAuth-Zugang und Refresh-Token, Aktivierung/WLAN-Optionen |

Die Bilder gehören zum Profilbereich, der Kartenschlüssel zum Kartenbereich,
Dienstzugangsdaten zum Cloud-Bereich. Eine Teilwiederherstellung installiert nur
die gewählten Bereiche. Die Profilwiederherstellung projiziert den gespeicherten
aktiven UI-/Karten-Snapshot auch in die laufenden Einstellungen; fehlende Overrides
werden entfernt und nicht aus dem Zielprofil übernommen. Einstellungen und Fotos
werden bei Schreibfehlern nach Möglichkeit gemeinsam zurückgerollt; gescheiterte
Rücknahme wird ausdrücklich als Fehler gemeldet.

Bei der Prüfung ergänzt: lokale Exportordnerreferenz; Kartenposition/Zoom/Trackwahl
im eigenständigen Kartenbereich; tatsächliche aktive Projektion der Profilbank;
Fahrradbilder und zuvor ausdrücklich ausgeschlossene Zugangsdaten. Neue/geänderte
Einstellungen müssen diesen expliziten Umfang und die Regressionstests mitpflegen.
Keine pauschale Kopie von preferences.all.

## Gerätewechsel und Grenzen

Zugangsdaten werden für den Export unter dem Quellgeräteschlüssel entschlüsselt und
innerhalb des Passwort-ZIP übertragen. Beim Import verschlüsselt StoredSecret sie
unter dem Zielgeräteschlüssel neu. Android-Keystore-Schlüssel werden nicht kopiert.
Nicht lesbare vorhandene Zugangsdaten lassen den Export mit verständlichem Fehler
scheitern, statt unbemerkt zu fehlen. Bereits abgelaufene/widerrufene Dienstzugänge
können natürlich eine erneute Anmeldung erfordern.

Android-Berechtigungen (Bluetooth, Standort, Benachrichtigungszugriff, Ordnerzugriff)
sind nicht übertragbar. Ordnerreferenzen bleiben erhalten, die Freigabe muss auf
einem neuen Gerät ggf. erneut durch Auswahl des Ordners erteilt werden.
Laufende Aufnahmen, Live-Sensorwerte, Scanstatus, OAuth-Anfragen, Uploadwarteschlangen
und Erfolgsbelege sind keine einstellbaren Felder und werden nicht übertragen.
HRV-Messungen/Tagesbefinden und Fahrten gehören zur vollständigen Datensicherung,
nicht zur reinen Einstellungssicherung.

Die vollständige Lokal-/WebDAV-Verzeichnissicherung enthält nun ebenfalls
`settings.zip` nach Passwortabfrage. FIT-/GPX-/HRV-Dateien, Manifest und separate
Bilder daneben bleiben unverschlüsselt; die Passwortverschlüsselung schützt die
Einstellungssicherung einschließlich Zugangsdaten. Alte Verzeichnisse mit
`settings.json` bleiben kompatibel. Fahrtwiederherstellung braucht kein Passwort,
die separate Einstellungswiederherstellung entsperrt das ZIP.

## Gezielte Prüfung

SettingsArchiveTest: AES-256-Header, Vollständigkeitsrundlauf über verschiedene
Geräteschlüssel/aktive und inaktive Profile/Fotos, Teilrestore, falsches Passwort,
manipuliertes Archiv, ungeeignete ZIP-Einträge, unlesbare Quellzugänge, alte JSONs
und Entfernen nicht gesicherter Ziel-Overrides. SettingsPasswordViewTest prüft
Passwortlänge/Bestätigung, deaktiviertes View-State/Autofill und Leeren beim Schließen.
Bestehende SettingsBackup-/Supplement-/RideDirectoryBackup-Tests erhalten; ergänzter
Verzeichnisrundlauf mit settings.zip und FIT-Wiederherstellung ohne Passwort.
DataSettingsNavigationTest prüft SAF-Abbruch, falsches Passwort, Vorschau/Bestätigung
und Rückkehr nach Activity-Neuerstellung ohne Passwort/Archiv im Bundle.
Geräte-/WebDAV-Integration mit echten Zugangsdaten nicht in der Cloud getestet.

Stand 23.10.2026: die beschriebenen Änderungen sind in APK **0.8.32 / Build 71** enthalten. [Buildnachweis](build-71.md).
