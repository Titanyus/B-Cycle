# B-Cycle 0.7.10 – Test-APK

Stand: 13.10.2026. Funktionsstand: `880d25c3fbb71070ec12e175cc4f0aedcd1bbcc7`.
Für die APK wurden nur die Versionsangaben und Release-Dokumentation aktualisiert.

- Version 0.7.10, versionCode 48; Paket `de.bcycle.app`.
- Android 13+ (minSdk 33), Compile-/Target-SDK 36.
- APK: `downloads/B-Cycle-0.7.10-test.apk`; 1.074.873 Byte, rund 1,07 MB.
- SHA-256: `e2bc75ba9c2ace5f6826ea6ad3ec24c197edd915ce5fb783154de0f1cf25f7bb`.
- Signatur: APK Signature Scheme v3, bisheriger Android-Testschlüssel; Update-installierbar.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.

## Enthaltene Änderungen

- Direktwert-Chevrons verwenden den zugehörigen bestehenden Durchschnittstrend;
  GPS-Geschwindigkeit folgt dem Durchschnitt in Bewegung.
- Letzte Glow-Helligkeitserhöhung zurückgenommen (175 → 140); Breite beibehalten.
- Graphlinien 2,6 dp mit kräftigem Kern und sanftem Lichtsaum.
- Flächenfüllung läuft nach unten transparent aus, dünne Grenzlinien nach oben/unten.
- Graph-Skalenschrift 80 Prozent größer, kleine farbige Overlay-Punkte neben der Feldbezeichnung.
- Hauptmesswerte, Sensorverarbeitung, FIT-Rohdaten und Performance Engine unverändert.

## Prüfung

```sh
./gradlew :app:testDebugUnitTest --tests 'de.bcycle.app.FitWriterTest' :app:assembleRelease --console=plain
```

**6 Tests bestanden, 0 Fehler, 0 übersprungen**, Release-Build mit R8, Resource-Shrinking
und Release-Lint-Vital erfolgreich in **39 Sekunden**. Die Tests prüfen FIT-Versionsangaben,
Garmin-Dekodierung und Validierung. Signatur, ZIP-Ausrichtung, Version und SDK-Werte separat geprüft.

Vorausgehende Änderungen wurden bereits gezielt geprüft: acht Chevron-/Graph-Tests,
zwei Glow-Pixeltests sowie sieben unterschiedliche native Tests der weiteren Graph-/Kartenanzeige.
Keine erneute vollständige Suite bei dieser APK-Erstellung. Die ursprüngliche Graph-/SDK-Basis
hatte zuvor die vollständige Suite mit 336 Tests bestanden; das ist kein neuer Vollsuite-Lauf
für die jetzigen Darstellungskorrekturen.

[Graph-Implementierung und Detailnachweis](live-graph-overlays.md), [Randglühen](mode-glow.md).
Private FIT-Dateien, Keystore, SDK und Build-Caches werden nicht veröffentlicht.
