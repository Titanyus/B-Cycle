# B-Cycle 0.7.13 – Test-APK

Stand: 14.10.2026. Funktionsstand: `7b1770e0262c00bb0fcce4b0c7bf84d988388fa5`.
Für diesen APK-Auftrag wurden nur Versionsangaben und Veröffentlichung aktualisiert.

- Version 0.7.13, versionCode 51; Paket `de.bcycle.app`.
- Android 13+ (minSdk 33), Compile-/Target-SDK 36.
- APK: `downloads/B-Cycle-0.7.13-test.apk`; **1087274 Byte**.
- SHA-256: `57b78752fbf1ef4e40b82de54efff51435c2c9ce1fcc2183e4aa86d1917d442f`.
- Signatur mit APK 0.7.12 verglichen: identischer bisheriger Android-Testschlüssel.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.

## Enthaltene Änderungen

- Abschnittsüberschriften appweit 23 sp und zentrale System-Fettschrift mit Gewicht
  900 Black (Roboto auf Standard-Android, System-Ersatzschrift je nach Gerät).
- Nur im Haupt-Einstellungsmenü: Schrift 28 sp und führende Menü-Icons 48 dp.
- Graph-Darstellung direkt im Datenauswahl-Dialog pro Dashboardseite/Kachel;
  alte globale Auswahl dient als Ausgangswert. Speicherung und Backup kompatibel.
- Sonne/Mond links oben neben dem Track-Auge, keine zweite rechte Buttonzeile.
- Vorherige Graph-Helligkeit, Leuchtsaum, Sensorwerte und FIT-Aufzeichnung erhalten.

## Prüfung

Sechs FitWriterTest-Prüfungen und Release-Build erfolgreich in **33 Sekunden**
mit wiederverwendeten Build-Zwischenergebnissen. Release-Lint-Vital, R8 und
Resource-Shrinking erfolgreich. Signatur, ZIP-Ausrichtung und Paket-/SDK-Metadaten
separat geprüft. Die vorausgehenden vier gezielten UI-Prüfungen für Menügrößen,
Kachelmodi/Backup, 23-sp-/900-Typografie und Kartenposition/Nachtzustand haben bereits
bestanden (12 Sekunden). Keine erneute vollständige Testsuite für den APK-Auftrag.
Ein Praxistest auf dem Telefon steht aus.

Private FIT-Dateien, Keystore und Build-Caches werden nicht veröffentlicht.
