# Test-Build 15 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-15-apk/downloads/B-Cycle-0.7.30-test-build-15.apk)

## Änderungen seit Test-Build 9

- Trainingshilfe dauerhaft unter Daten und Graph in jeder Feldauswahl sowie in Design & UI.
- Einheitlicher Long-Press auf obere Dashboard-Felder öffnet den Editor; kein messwertabhängiger Sonderzugriff.
- Feldauswahl bleibt nach Änderungen geöffnet, bis der Nutzer zurückgeht.
- Layout-Auswahl mit dreispaltigem Vorschau-Kartenraster; Symbolbilder um 60 % vergrößert und auf schmalen Displays proportional begrenzt.
- Erklärende Formulartexte mit zentralem 8-dp-Mindestabstand zum vorherigen Element, einschließlich Benachrichtigungshinweis unter „Android-Zugriff verwalten“.

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 15](https://github.com/Titanyus/B-Cycle/actions/runs/39007486467), Quellstand `189bc5e490513b2322740d36f73e8b5fe90b561b`.
Vollständige automatisierte Unit-/Robolectric-Testsuite: **571 Tests, keine Fehler, keine übersprungenen Tests**.
Drei überholte UI-Testannahmen wurden an die vorhandene Trainingshilfe und aktuelle Feldauswahl angepasst.

Die APK wurde von GitHub Actions gebaut und anschließend ausschließlich mit dem bisherigen Testschlüssel nachsigniert. Kein lokaler APK-Build.
App-Version **0.7.30**, versionCode **68**. Als Update über die bisherigen mit diesem Testschlüssel signierten APKs installierbar.
Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein zusätzlicher Gerätetest in dieser Umgebung.

Größe: 8,152,926 Bytes.

SHA-256: `f6557e501a6a63ec654a2a9eeec8efda2efe9f149ca999b69d9bb8e0b51dd85b`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.

Vorgemerkt: Schalterspur besser sichtbar machen; aktuelle Farben bleiben in diesem Build unverändert.
