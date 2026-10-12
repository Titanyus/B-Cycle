# B|Cycle 0.7.3 – Teststand Streckendetails

Tag `v0.7.3` enthält den vollständigen Quellcode, Ressourcen, Tests, Dokumentation und die signierte Test-APK. Alte Tags/APKs bleiben erhalten.

Neue Funktion: [Streckendetails V1](route-analysis-v1.md). Danach wurde die [Dashboard-Attribution](map-bottom-layout.md) nach links unten gedreht. Vor der Kartenänderung bestanden alle 250 Tests, Release-Build erfolgreich in 1m 8s. Anschließend bestanden zwei gezielte Kartenlayouttests und ein zusätzlicher Karten-Lifecycle-Test; abschließender Release-Build erfolgreich in 48s. Kein erneuter vollständiger Suite-Lauf nach diesen letzten Änderungen, entsprechend dem Wunsch nach minimalen Tests für die Kartenänderung.

Release-Lint-Vital erfolgreich. Bekannte Produktionswarnung `onBackPressed()`; native Android-Zurück-Tests verwenden ebenfalls diese API. Version und Signatur separat geprüft. Kein vollständiger zusätzlicher Lint-Lauf und kein Fahr-/Gerätetest.

VersionName 0.7.3, VersionCode 41; minSdk 33, targetSdk 35.
APK 988437 Byte. Signatur entspricht bisherigen Test-APKs.
SHA-256 `5a5ce402f9cc4383fa78acb3077d3f5d0cd2027d34299e6b1c1ad35856a02ddb`.

Praktisch zu prüfen: Details realer Fahrten, Diagrammlücken, Umschalter, Scrollen und Zurück, Karten-Vollbild, lesbarer gedrehter Quellenhinweis. Keine privaten Messdateien, Caches, SDKs oder Keystores veröffentlicht.
