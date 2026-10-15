# B-Cycle 0.7.21 – Test-APK

Stand: 15.10.2026. Quellbasis a651438 mit Versionsanhebung.
Version 0.7.21 / versionCode 59; Android 13+, Compile-/Target-SDK 36.

## Änderungen seit 0.7.20

- Gemeinsame Sticky-Kategorien für Fahrer, Bikes, Daten & Speicher und Design & UI.
- Design & UI: Theme, Dashboard-Layout und Fahrmodus-Anzeige.
- Einzeilige numerische Eingaben mit Bebas Neue in 28 sp, Einheit dahinter,
  transparentem Hintergrund und 1-px-TERTIARY-Rahmen auf Fahrer und Bikes.
- WebDAV-Eingaben mit dezentem Rahmen und normaler Systemschrift.
- FIT-Aufzeichnung als fester Abschnitt; zusätzliche FIT-Datenquellen jeweils
  in einem eigenen Accordion. Auswahl und Offen-Zustand bleiben beim Bikewechsel erhalten.
- Fünf Strecken-Verlaufsdiagramme nur mit deckender, geglätteter Flächenfüllung,
  ohne separate Konturlinie. Glättung, Nullwerte und Datenlücken bleiben erhalten.
- Karten unverändert.

## Validierung

Die UI-Umstellung wurde vor diesem Build mit 25 gezielten Tests geprüft;
zusätzlich abschließender Integrationstest und Release-Kotlin-Kompilierung.
Für die Diagrammanpassung bestanden danach 3 Kurventests und die
Release-Kotlin-Kompilierung. Keine erneute vollständige Testsuite für den APK-Build.
Keine Installation auf einem realen Gerät getestet.

Optimierter Release-Build mit R8, Resource-Shrinking und Lint-Vital erfolgreich:
52 Sekunden. APK signiert; Signatur, Zertifikatgleichheit mit 0.7.20,
ZIP-Ausrichtung (16 KiB), Paket-, Versions- und SDK-Metadaten geprüft.

APK: `downloads/B-Cycle-0.7.21-test.apk`, 1177554 Byte.
SHA-256: `a592541a3ca8873492f3d8d2b0c32e272536d7405ae98e404b625abb177c8713`.
Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
Mit bisherigem Testschlüssel als Update installierbar.
