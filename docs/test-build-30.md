# Test-Build 30 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-30-apk/downloads/B-Cycle-0.7.31-test-build-30.apk)

## Benachrichtigungen: App-Auswahl filtern

Die vollständige App-Auswahl blendet Android-Systemdienste ohne eigenen App-Einstieg aus. Vorinstallierte reguläre Apps mit App-Einstieg und bekannte Messenger bleiben auswählbar. Der zusätzliche Messenger-Filter bleibt erhalten. Bereits gespeicherte Auswahlen werden durch das Filtern nicht gelöscht; auch beobachtete Systemdienste umgehen den Filter nicht. Keine Änderung am Nachrichtenempfang.

## Nur vorgemerkt

- Dashboard im Vollbild als ersten Inhaltspunkt in Design & UI setzen.
- Benachrichtigungen: Header Position/Anzeigedauer entfernen; Titel links neben Slider, Sekundenwert darüber.
- Button-/Dropdown-Rahmen auf 1 dp und FOREGROUND mit 55 % Deckkraft wie die Sensor-Trennlinien ändern. SECONDARY und 6-dp-Rundung erhalten.

Diese Änderungen sind noch nicht umgesetzt; Build 30 verwendet weiterhin 2 dp und volldeckende FOREGROUND-Konturen.

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 30](https://github.com/Titanyus/B-Cycle/actions/runs/39054523208), Quellstand `3388f2cbd8b3d0ec466d545e61fd30868170f863`.

**8 gezielte Tests erfolgreich**, keine Fehler oder übersprungenen Tests. Geprüft: Systemdienste, vorinstallierte reguläre Apps, Messenger, Filterwechsel und Erhalt gespeicherter Auswahlen. Keine vollständige Testsuite und kein Gerätetest.

APK durch GitHub Actions gebaut, anschließend mit dem bisherigen Testschlüssel nachsigniert. Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein lokaler APK-Build.

Version **0.7.31**, versionCode **69**; als Update über bisherige mit diesem Testschlüssel signierte APKs installierbar.

Größe: 8,239,023 Bytes.

SHA-256: `df1ec2ec110249ed12f1af46072bf5026cb2f3df93f9e3d1f3c3b11047234b60`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
