# Test-Build 17 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-17-apk/downloads/B-Cycle-0.7.30-test-build-17.apk)

## Korrekturen seit Test-Build 15

- Vereinbarte gemeinsame Zonenpalette jetzt angewendet: Puls Z1–5 = #8696A5, #66BB6A, #FFCA28, #FF8A26, #EF5350; Leistung Z1–7 = #8696A5, #42A5F5, #66BB6A, #FFCA28, #FF8A26, #EF5350, #AB47BC.
- Die zentralen Farben gelten im Fahrerprofil, bei Zonenbalken und Verteilungen sowie in der Trainingshilfe.
- Einheitliche Dashboard-Vorschaubilder: benannte Dashboard-Kacheln und Layout-Raster verwenden die bewährten 60 × 84 dp, wie vor Einführung dynamischer Dashboards. Die zuvor gewünschte Vergrößerung wurde auf erneuten Nutzerwunsch zurückgenommen. Bike-Kacheln unverändert.
- Text neben dem Icon im Layout-Dropdown vertikal zentriert, mit nativen Compound-Drawables statt eingebettetem ImageSpan.

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 17](https://github.com/Titanyus/B-Cycle/actions/runs/39008078032), Quellstand `ce82852b97ac0189c2892694b24a9a95005f30f2`.
Vollständige automatisierte Unit-/Robolectric-Testsuite auf diesem Quellstand: **571 Tests erfolgreich, keine Fehler oder übersprungenen Tests**.

Die APK wurde von GitHub Actions gebaut und anschließend ausschließlich mit dem bisherigen Testschlüssel nachsigniert. Kein lokaler APK-Build.
App-Version **0.7.30**, versionCode **68**. Als Update über die bisherigen mit diesem Testschlüssel signierten APKs installierbar.
Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein zusätzlicher Gerätetest in dieser Umgebung.

Größe: 8,152,926 Bytes.

SHA-256: `195c758ec58494a36648ef097c2f5c876514ae0c4fb3dbd78bfe91e51c64e3b3`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
