# B|CYCLE 0.8.3 – Build 38

Optimierter Build vom 18.10.2026; App-Version 0.8.3, versionCode 73. Neuer Quellstand gegenüber Build 37, separat als v0.8.3-build38 veröffentlicht. Mit gleichem Schlüssel und gleichem versionCode als Update installierbar.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.3-build38/downloads/B-Cycle-0.8.3-build38.apk)**

## Änderungen gegenüber Build 37

- Daten & Speicher: eigenes Accordion Streckenimport mit den drei bestehenden Importaktionen.
- Einheitliche Dropdown-Fenster: SECONDARY, 1 dp FOREGROUND-Rahmen mit 50 % Deckkraft und 6 dp Rundung; Rahmen vor Übermalen durch Listeninhalt geschützt.
- Leicht federndes Einrasten nach dem Umsortieren von Bike- und Dashboardkacheln, ohne neue Bibliothek.

## Prüfung

Gezielte Tests für Importauswahl und Erhalt der Datenseite, Dropdown-Komponenten, Bike-Schnellauswahl und Themeauswahl bestanden. Abschließend Kontur-Innenabstand, gemeinsame Geometrie, Drag & Drop inklusive Persistenz und Import-Dateiauswahl nach den letzten Änderungen geprüft. Kein voller Testlauf, kein physischer Gerätetest.

## Build und Download

- [Erfolgreicher GitHub-Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/39120259782), Quellstand 5c2e38f01de743520da94c0b2350db3c60dac208.
- Manueller optimierter Release mit R8 und Ressourcenverkleinerung, kein lokaler APK-Build.
- Artefakt-Prüfsumme vor Nachsignierung geprüft, etablierter Testschlüssel. Signatur, Version, 16-KiB-Zip-Ausrichtung und nicht-debuggable Manifest geprüft.
- Größe: 1,780,731 Bytes (rund 1.8 MB).
- SHA-256: `471b38bb1c8c16f7e04e95814c3dac5fe77e84bfdbd46918c09e5e13920355a2`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.3-build38/downloads/B-Cycle-0.8.3-build38.apk.sha256) · [Mapping](https://github.com/Titanyus/B-Cycle/blob/v0.8.3-build38/downloads/B-Cycle-0.8.3-build38-mapping.txt.gz).

Offen vorgemerkt: Dreeve-Bestätigungsstatus verbessern und spätere zentrale Strava-Anmeldung. Diese sind nicht Bestandteil dieses Builds.
