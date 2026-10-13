# B-Cycle 0.7.11 – Test-APK

Stand: 13.10.2026. Funktionsstand: `22639affdbcd5cba8b1d2cac27b1a5a42e3c1aef`.
Für die APK wurden ausschließlich die Versionsangaben und Release-Dokumentation aktualisiert.

- Version 0.7.11, versionCode 49; Paket `de.bcycle.app`.
- Android 13+ (minSdk 33), Compile-/Target-SDK 36.
- APK: `downloads/B-Cycle-0.7.11-test.apk`; 1.083.121 Byte, rund 1,08 MB.
- SHA-256: `c7144088edb07fe15ba599a4dcfd5bd580e209422b731dccbc94d57a56e59022`.
- Signatur: v3, bisheriger Android-Testschlüssel; Zertifikat mit APK 0.7.10 verglichen und identisch.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.

## Enthaltene Änderungen

- [Dashboard-Editor V2](dashboard-editor.md): Long-Press oder „Design & UI → Dashboard bearbeiten“;
  gemeinsame obere/Karten-Feldauswahl, PRIMARY-Rahmen, zentraler Beenden-Button, Bearbeitung auch während der Fahrt.
- [Weiche Kartenbewegung](map-motion.md): Kamera und Marker verwenden dieselben Anzeige-Zwischenpositionen,
  450-ms-Übergang, Abbruch bei manueller Bedienung, Stillstand, Dialogüberdeckung und App-Unterbrechung.
- Hochformat verbindlich in Manifest und App-Konventionen; UI-Prüfungen auf Hochformat umgestellt.
- Kleinere Graph-Anfangsbereiche, farbige Auswahlpunkte und Quellen-Icons im Datenfeld-Dropdown.
- Alle vorherigen Funktionen einschließlich Graphen, Glow, FIT und Sensorverarbeitung bleiben erhalten.

## Prüfung

```sh
./gradlew :app:testDebugUnitTest --tests de.bcycle.app.FitWriterTest :app:assembleRelease --console=plain
```

**6 Tests bestanden, 0 Fehler**, Release-Build mit R8, Resource-Shrinking und Release-Lint-Vital
in **45 Sekunden**. Signatur, ZIP-Ausrichtung und Paket-/SDK-Metadaten separat verifiziert.
Der zugrunde liegende Funktionsstand hat unmittelbar zuvor die **vollständige Suite mit 354 Tests**
sowie den Release-Build bestanden (1 Minute 57 Sekunden). Keine erneute vollständige Suite für
alleinige Versions-/APK-Erstellung. Praxistest auf dem Telefon steht aus.

Private FIT-Dateien, Keystore, SDK und Build-Caches werden nicht veröffentlicht.
