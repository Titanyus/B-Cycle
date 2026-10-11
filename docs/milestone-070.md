# Gesicherter Stand B|Cycle 0.7.0

Tag: `v0.7.0`. Der vollständige Meilenstein `v0.6.21` bleibt als Rückkehrpunkt erhalten.
Quellcode, Tests, Ressourcen, Gradle-Konfiguration, Dokumentation und signierte APK
sind zusammen gesichert. Lokale SDKs, Caches, Keystore und private Daten fehlen bewusst.

Änderungen: [kleine Altlastenbereinigung](cleanup-070.md). Keine Sensor-/Fahrfunktionsänderungen.
`:app:testDebugUnitTest :app:assembleRelease`: erfolgreich. 213 Tests, 0 Fehler,
0 Fehlschläge, 0 übersprungen. Erweiterte FIT-Prüfung decodiert Geräte-Softwareversion
und App-Buildnummer mit dem offiziellen Garmin-SDK; Activity-Validator weiterhin erfolgreich.
Release-Lint-Vital erfolgreich; kein neuer vollständiger Lint-Lauf. Bekannte
Compilerwarnung: veraltetes `onBackPressed()`.

VersionCode 38, VersionName 0.7.0, minSdk 33. Signatur entspricht der vorherigen APK.
APK: 934741 Byte. SHA-256: `4dce5ff37d6587005af93beeb443917e39541fd6484b5be16ca7ed9e3ad1b4e3`.
