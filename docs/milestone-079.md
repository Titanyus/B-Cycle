# B-Cycle 0.7.9 – Test-APK

Stand: 13.10.2026. Freigegebener Funktionsstand: `bf38b1557c7de1f3d682adddab58b22f8b02ebc5`.
Für diese APK wurden ausschließlich die Versionsangaben angehoben und die Release-Dokumentation aktualisiert.

- Version: 0.7.9, versionCode 47; Paket `de.bcycle.app`.
- Android 13+ / minSdk 33; Compile-/Target-SDK 36.
- Signierte APK: `downloads/B-Cycle-0.7.9-test.apk`.
- Größe: 1.074.873 Byte (rund 1,07 MB).
- SHA-256: `81f59aa5cf691a4de4d5bce98da3da5acddefc8bb33e68d4779370515c3d854e`.
- Signatur überprüft: APK Signature Scheme v3, bisheriger Android-Testschlüssel.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- Als Update über die bisherige Test-APK installierbar; ältere Downloads bleiben erhalten.

## Enthalten

Live-Graph-Overlays für große Dashboardfelder mit bis zu zwei Datenreihen, sanfter visueller
Glättung, gemeinsamer Distanzachse und zehnsekündlicher Anzeigeübernahme. Auswahl je Feld/Seite
und Dashboard-Backup. Sensorwerte, Live-Zahlen, FIT-Rohdaten und Performance-Berechnungen bleiben
unverändert. Größere Zahlen in Dreier-Reihen sowie die native SDK-36-Zurück-Anbindung sind enthalten.

[Implementierungsnachweis und vollständige Tests](live-graph-overlays.md).
Die UX-/Konfigurationsgrundsätze sind in `AGENTS.md` festgehalten.

## Validierung

```sh
./gradlew :app:testDebugUnitTest --tests 'de.bcycle.app.FitWriterTest' :app:assembleRelease --console=plain
```

**6 Tests bestanden, 0 Fehler, 0 übersprungen**, Release-Build mit R8/Resource-Shrinking
und Release-Lint-Vital erfolgreich in **37 Sekunden**. Die gezielten Tests prüfen auch die
Versionsangaben im FIT-Export mit Garmin-Decoder und Validator. Der unveränderte Funktionsstand
wurde unmittelbar zuvor mit der vollständigen Suite von **336 Tests** geprüft; diese Suite
wurde für die reine Versionierung nicht erneut ausgeführt.

APK-Ausrichtung, Signatur, Paketversion und SDK-Werte wurden anschließend separat überprüft.
Private FIT-Dateien, Keystores, SDK-Dateien und Build-Caches werden nicht veröffentlicht.
Reale BLE-/GPS-Fahrten und Android-16-Hintergrundverhalten bleiben Bestandteil des Gerätetests.
