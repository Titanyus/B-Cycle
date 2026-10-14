# B-Cycle 0.7.12 – Test-APK

Stand: 14.10.2026. Basis: `2cf71f28d1165286293ec84c32389b07c244827f`.

- Version 0.7.12, versionCode 50; Paket `de.bcycle.app`.
- Android 13+ (minSdk 33), Compile-/Target-SDK 36.
- APK: `downloads/B-Cycle-0.7.12-test.apk`; **1087274 Byte**.
- SHA-256: `6b89a133dbda0d62b5935188fed24ccbac4f7e1fd64f8a2e87e58c175ec09cd3`.
- Signatur mit APK 0.7.11 verglichen: identischer bisheriger Android-Testschlüssel.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.

## Änderungen

- Globale Graph-Darstellung in Design & UI: Graph mit Daten oder Nur Graph.
- Automatische Speicherung und Integration in die bestehende Design-&-UI-Sicherung,
  kompatibel mit alten Sicherungen ohne Darstellungsmodus.
- Farbverlauf 1,8-fache Deckkraft. Bei Textüberlagerung Linie 70 % und Füllung 85 %
  der neuen vollen Deckkraft. Hauptlinie 2,6 dp, Leuchtsäume 11,2/19,2 dp.
- Alle vorher gesicherten [UI-Navigationsänderungen](ui-navigation.md) sind enthalten.
- Keine Änderung an Sensorwerten, FIT-Daten, Performanceberechnung oder Graph-Takt.

## Prüfung

42 gezielte Tests, 0 Fehler/Fehlschläge/übersprungen, und Release-Build erfolgreich.
Klassen: LiveGraphUiTest, DashboardEditorTest, SettingsBackupTest, UiConventionsTest,
FitWriterTest. Pixelprüfungen sichern Helligkeitsunterschied, Leuchtsaumgrenzen,
Clipping und Lücken; UI-Prüfungen sichern alle Presets, Beschriftungen, sofortige
Persistenz und Sicherungs-Kompatibilität. Abschlusslauf **29 Sekunden** mit
wiederverwendeten Build-Zwischenergebnissen. Release-Lint-Vital, R8 und
Resource-Shrinking erfolgreich; Signatur, ZIP-Ausrichtung und Metadaten geprüft.

Der vorherige UI-Stand hat bereits die vollständige Suite mit 357 Tests bestanden.
Für diese abgegrenzte Graph-Anpassung wurde die vollständige Suite nicht erneut
aufgerufen. Ein Praxistest auf dem Telefon steht aus.

Private FIT-Dateien, Keystore und Build-Caches werden nicht veröffentlicht.
