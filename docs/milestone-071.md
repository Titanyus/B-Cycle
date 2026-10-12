# Gesicherter Stand B|Cycle 0.7.1

Tag: `v0.7.1`. Frühere Tags und APKs bleiben erhalten. Vollständiger Quellcode, Tests,
Ressourcen, Gradle-Konfiguration, Dokumentation und signierte Test-APK sind gemeinsam gesichert.

Enthält die manuelle Nachtkarte, Vollbild-Einstellungsansichten, Color Engine V2,
Aufnahmezeitkorrektur, bestätigte Ausrollphasen in der Performanceberechnung und Akku-Breathing.
Details und Grenzen stehen in den zugehörigen Dokumenten und der README.

`:app:testDebugUnitTest :app:assembleRelease`: erfolgreich in 1m 34s.
239 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen; 49 XML-Testberichte.
Release-Lint-Vital erfolgreich. Kein zusätzlicher vollständiger Lint-Lauf.
Bekannte Compilerwarnung: veraltetes `onBackPressed()`.

VersionCode 39, VersionName 0.7.1, minSdk 33, targetSdk 35.
APK 951293 Byte. Signatur geprüft und identisch mit der bisherigen Test-APK.
SHA-256: `181c474a6013cfe845d6f5df62cf2fe82ec55ad283baab540df6e28294c768de`.

Kartenpalette, Flüssigkeit, reales Fahrrad und Bildschirm-/App-Wechsel sind am Telefon zu prüfen.
Tests verwenden JVM und native Android-Grafik über Robolectric; dies ersetzt keinen Fahrtest.
Lokale SDKs, Caches, Keystore und private FIT-Dateien werden nicht veröffentlicht.
