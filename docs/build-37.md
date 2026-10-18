# B|CYCLE 0.8.3 – optimierte APK

Build 37 vom 18.10.2026, versionCode 73.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.3/downloads/B-Cycle-0.8.3.apk)**

## Änderungen

- Android-Tastatur für nachträglich eingefügte Cloudformulare freigegeben: WebDAV, intervals.icu, Dreeve und Strava.
- Gemeinsame Dialogkorrektur für dynamische Texteingaben, kompakte Dialoge und Rückkehr von Unterseiten.
- Zentrale Inhaltsüberschriften im Stil der Hilfe (14 sp, Roboto 900 Black), auch unter Erweitert → Quellen.
- Obere Footer-Trennlinie mit 1 dp in FOREGROUND, einschließlich Profil-/Dashboardfooter.

## Prüfung

Vier gezielte Tests bestanden: zehn Cloud-Eingaben, Fahrerprofil, Profilname/Radumfang, Karten-API-Schlüssel, Dashboard- und Streckenumbenennung sowie nachträglich eingefügte Editoren in Vollbild-/kompakten Dialogen und Rückkehr vom Kinddialog. Native Editor-Eigenschaft, Fokus, Tastaturfreigabe, InputConnection und Fensterflags geprüft. Die Cloudregression scheitert ohne Korrektur und besteht mit Korrektur. Kein voller Testlauf und kein physischer Gerätetest; die tatsächliche Android-Bildschirmtastatur muss am Gerät geprüft werden. Vorangehende Überschriften-/Footerprüfungen ebenfalls bestanden.

Details: [Texteingabe](settings-text-input.md).

## Build

- [Erfolgreicher GitHub-Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/39110405049), Quellstand 597c2fa6513ff6943509188e88fcab28b03d1400.
- Release mit R8 und Ressourcenverkleinerung; kein lokaler APK-Build.
- Artefakt-Prüfsumme vor Nachsignierung geprüft. Etablierter Testschlüssel, Version 0.8.3/73, 16-KiB-Zip-Ausrichtung und nicht-debuggable Manifest geprüft.
- Größe: 1,780,731 Bytes (rund 1.8 MB).
- SHA-256: `e573b34cfa3df52218aeac90fa362209dd19db662f5d10be7d1a5fc8420ae5f9`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.3/downloads/B-Cycle-0.8.3.apk.sha256) · [R8-Mapping](https://github.com/Titanyus/B-Cycle/blob/v0.8.3/downloads/B-Cycle-0.8.3-mapping.txt.gz).

Dauerhafter APK-Download am Versionstag. Meilenstein 0.8 bleibt erhalten.
