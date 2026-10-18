# B|CYCLE 0.8.1 – optimierte APK

Build 35 vom 18.10.2026, versionCode 71.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.1/downloads/B-Cycle-0.8.1.apk)**

## Änderungen in 0.8.1

- Helle farbige Themes: sichtbare Auswahlrahmen und hellere TERTIARY-Flächen.
- Lesbarer Datumsdialog im Hellmodus; einheitliche Auswahl-Schrift, Sensoricons und größere Abstände zu Auswahlkreisen.
- Trainingshilfe als „Aus | Alle Zonen | Zone 2“, gemeinsam in Design & UI und Feldauswahl.
- Indoor-Unterauswahl Hometrainer; Rollentrainer vorbereitet und deaktiviert.
- Bosch-Fahrmodus als Schalter-/Accordion-Zeile am Ende von Design & UI.
- Übersetzbare lokale Hilfen über Info im Header; kurze Erklärung zur Strecken-Aufzeichnung.
- Streckendetails: Anzeigen, Umbenennen, Höhe korrigieren, Upload, Löschen. Titel weiterhin direkt zum Umbenennen antippbar.
- Upload-Dropdown mit Einzelstatus und Erfolgs-Haken je Ziel, manueller lokaler Speicherung sowie direktem Konfigurieren.
- FIT-Gerätename B|CYCLE, mit offiziellem Garmin-Decoder geprüft.

## Prüfung

Die zugehörigen gezielten UI-, Persistenz-, Uploadbeleg- und Navigationstests sowie der Garmin-FIT-Validierungstest bestanden. Keine erneute vollständige Suite für diesen Build; der Meilenstein 0.8 wurde mit 608 Tests geprüft. Kein physischer Gerätetest dieser APK.

## Build und Downloadprüfung

- [Erfolgreicher GitHub-Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/39102855713), Quellstand `78b7976070e905fdba28887f22ae5f5cc144cfb4`.
- Release mit R8 und Ressourcenverkleinerung; kein lokaler APK-Build.
- Artefakt vor Nachsignierung per SHA-256 geprüft, anschließend mit dem bisherigen Testschlüssel signiert. Zertifikat, Version und 16-KiB-Zip-Ausrichtung geprüft; Manifest nicht debuggable.
- Größe: **1,764,347 Bytes** (rund 1,8 MB).
- SHA-256: `a881c60f90f5871c55c9cd672a3bc73e350e22c5f0104800566355ce2a2ff088`.
- Signaturzertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.1/downloads/B-Cycle-0.8.1.apk.sha256) · [R8-Mapping](https://github.com/Titanyus/B-Cycle/blob/v0.8.1/downloads/B-Cycle-0.8.1-mapping.txt.gz).

Der APK-Download liegt dauerhaft am Versionstag im Repository. Der Meilenstein 0.8 bleibt erhalten.
