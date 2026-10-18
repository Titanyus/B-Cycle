# B|CYCLE 0.8.2 – optimierte APK

Build 36 vom 18.10.2026, versionCode 72.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.2/downloads/B-Cycle-0.8.2.apk)**

## Änderungen

- Pulsmodelle Karvonen, Maximalpuls und Schwellenpuls (Friel Rad) mit passenden aktiven Eingabefeldern und gemeinsamer Zonenauswertung.
- Inaktive Pulsparameter bleiben gespeichert; Methode und Schwellenpuls werden gesichert.
- Hilfe nach Hauptabschnitten der Einstellungsseiten gruppiert; ausführliche Erklärung der Pulsmodelle.
- Profilfooter wechselt beim Scrollen zu Dashboardaktionen.
- Trainingshilfe mit aufklappbarer Auswahl für aktuelle und Durchschnittswerte.
- Altersanzeige unter dem Geburtsdatum ausgerichtet.

## Prüfung

Gezielte Tests für Zonengrenzen, modellabhängige Zone 2, Farbübergänge, Eingabeaktivierung, Werterhalt, Sicherung, Altprofil-Migration, Vorschau und Hilfegruppierung bestanden. Zugehörige Footer-/Trainingshilfe-/Altersanzeigetests ebenfalls bestanden. Keine erneute vollständige Suite; kein physischer Gerätetest.

## Build

- [Erfolgreicher GitHub-Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/39104523408), Quellstand cc1e6a3d99731866a2da56e0a7258d5969ee9a27.
- Release mit R8 und Ressourcenverkleinerung. Kein lokaler APK-Build.
- Artefakt-Prüfsumme vor Nachsignierung geprüft; etablierter Testschlüssel für Update-Kompatibilität. Signatur, Version 0.8.2/72, 16-KiB-Zip-Ausrichtung und nicht-debuggable Manifest geprüft.
- Größe: 1,780,731 Bytes (rund 1.8 MB).
- SHA-256: `5f5c0b3e6fc1c7bf1b2c80e7e1aa418888a7e6ed61449df5a07a681c53ec7ea4`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.2/downloads/B-Cycle-0.8.2.apk.sha256) · [R8-Mapping](https://github.com/Titanyus/B-Cycle/blob/v0.8.2/downloads/B-Cycle-0.8.2-mapping.txt.gz).

APK dauerhaft im Versionstag gesichert; Meilenstein 0.8 bleibt erhalten.
