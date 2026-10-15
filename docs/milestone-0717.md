# B-Cycle 0.7.17 – Test-APK

Stand: 15.10.2026. Hauptbranch-Basis: `68cc1f2`.

- Version 0.7.17 / versionCode 55; Paket `de.bcycle.app`.
- Android 13+, minSdk 33, Compile-/Target-SDK 36; native Views, Hochformat.
- Optimierte Release-APK: `downloads/B-Cycle-0.7.17-test.apk`, 1173458 Byte.
- SHA-256: `42c50368ee2f8e6e095129ce03397bec5491e838062e2853913d16571185013c`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- Signatur identisch mit 0.7.16; ZIP-Ausrichtung (16 KiB), Paket-, Versions- und SDK-Metadaten geprüft.

## Änderungen seit 0.7.16

Deckende Zonenflächen ohne Verlauf, weiße Kurvenfüllung unverändert. Sanfte
Anzeige-Glättung und monotone kubische Übergänge der fünf Verlaufsdiagramme,
kräftigere semantische Flächenfüllungen. Nullwerte, Datenlücken, Rohdaten und
Berechnungen bleiben erhalten. Neue Menübezeichnungen Karten/Strecken und
Reihenfolge Fahrer, Bikes, Strecken, Karten, Design & UI, Daten & Speicher, Erweitert.

Neue Preset-Namen EMBERS/FIRE, gespeicherte IDs bleiben erhalten. EMBERS verwendet
#BF281B für TERTIARY/ACCENT. FIRE verwendet exakt #535353/#000000/#FFB400/#FFFFFF/#FFB400
in Rollenreihenfolge PRIMARY/SECONDARY/TERTIARY/FOREGROUND/ACCENT. Die am 15.10.2026
bestätigte geringe Kontraststärke auf der gelben Fläche wird ausschließlich über
Preset-Metadaten für TERTIARY akzeptiert. Keine namensabhängige Layout-/Headerlogik;
übrige Validierung und strikte Custom-Theme-Prüfung bleiben erhalten.

## Validierung

Release-Build einschließlich R8, Resource-Shrinking und Lint-Vital erfolgreich,
gemessene Dauer 40.7 Sekunden. APK signiert; Signatur, Zertifikatvergleich,
ZIP-Ausrichtung und Versions-/SDK-Metadaten anschließend erfolgreich geprüft.

Keine neue vollständige Testsuite für die reine APK-Erzeugung. Änderungen zuvor
gezielt geprüft: native Zonenflächen, 13 Tests für Kurvenglättung und bestehende
Details-Layouts, Einstellungen/Shortcut/Sortierung sowie Theme-Validierung,
Theme-Manager und Theme-Auswahl einschließlich Neustart. Release-Kompilierungen
waren ebenfalls erfolgreich. Keine privaten FIT-Dateien, Schlüssel oder Zugangsdaten
veröffentlicht. Installation und praktische Fahrt auf echtem Android-Gerät hier
nicht durchgeführt.
