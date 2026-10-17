# B|CYCLE 0.8 – Meilenstein

Version 0.8, versionCode 70. Veröffentlichung am 18.10.2026.

## Änderungen

- Kein Glow an Puls-/Leistungsverteilungskurven in hellen Einstellungen; Hauptkurve und Füllverlauf bleiben erhalten.
- „Norm. Leistung“ als kürzere Beschriftung in Streckendetails.
- Weiße Symbole in PRIMARY-Seitenheadern auch bei heller Darstellung.
- Lokale Speicherung als Inline-Schalter/Accordion unter Daten & Speicher, einschließlich Ordnerauswahl.
- Alle Sticky-Abschnitte im Fahrerprofil auf-/zuklappbar.
- Vollständig sichtbarer Rahmen am Geburtsdatumsfeld.

## Validierung und Build

- Vollständige JVM-/Robolectric-Testsuite: **608 Tests bestanden**, keine Fehler, keine übersprungenen Tests.
- Optimierter Release-Build mit R8 und Ressourcenverkleinerung über [GitHub Actions, Lauf 34](https://github.com/Titanyus/B-Cycle/actions/runs/39073752384).
- Quellstand: `bbc187d781e66b8bfb2f7df88f101b9746c0973b`.
- Kein lokaler APK-Build. Actions-Artefakt vor Nachsignierung per SHA-256 geprüft.
- APK nach Download mit etabliertem Testschlüssel nachsigniert; Zertifikat und 16-KiB-Zip-Ausrichtung geprüft. Als Update zu bisherigen Test-APKs installierbar.
- Release-Manifest geprüft: Version 0.8 / Code 70, nicht debuggable.
- Noch kein physischer Gerätetest dieser optimierten APK; BLE, FIT und Netzwerkdienste müssen am Gerät bestätigt werden.
- R8-Mapping für spätere Fehleranalyse als komprimierter Release-Anhang gesichert.

## Download

[Optimierte APK herunterladen](https://github.com/Titanyus/B-Cycle/releases/download/v0.8/B-Cycle-0.8.apk)

Größe: **1.719.291 Bytes**, gegenüber 8.255.407 Bytes in Build 33 rund **79 % kleiner**.

SHA-256: `99c138e047ce66e8293ad28b587686bfaae2fb13349928f43ee264e449918855`

Signaturzertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
