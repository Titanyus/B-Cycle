# B|Cycle 0.7.5 – Detailaktionen und kompakter Streckenmanager

Tag `v0.7.5` sichert Quellcode, Tests, Ressourcen, Dokumentation und die signierte Test-APK. Vorherige Stände bleiben erhalten.

Änderungen: [Detail-Footer und bestätigte Erfolgsstatus](route-detail-actions.md), [kompakte Streckenliste](route-manager-step2.md).

Das Auge neben dem Streckentitel blendet den Track wie bisher auf der Dashboard-Karte ein/aus; es öffnet weder Details noch ein eigenes Kartenfenster. Ein Tipp auf die übrige Zeile öffnet die Details. Distanz, Bewegungszeit und Bewegungsdurchschnitt stehen kompakt nebeneinander. Alle fünf Verwaltungsfunktionen bleiben im Detail-Footer erhalten.

Vollständige Testsuite nach der Augen-Korrektur und Versionierung: 267 Tests bestanden, 0 Fehler, 0 Fehlschläge, 0 übersprungen. Release-Build und Release-Lint-Vital erfolgreich. Bekannte Produktionswarnung `onBackPressed()` bleibt; kein zusätzlicher vollständiger Lint-Lauf oder Gerätetest. Reale Nextcloud- und Dateiprovider-Vorgänge sind zusätzlich am Telefon zu prüfen.

APK: 1005073 Byte. SHA-256 `4cdb67ae905d9b415852db99e888af749d25f859f320b94dff977c25e290039e`.
Signatur geprüft und identisch mit dem bisherigen Testschlüssel, als Update installierbar.
MinSdk 33, TargetSdk 35, VersionName 0.7.5, VersionCode 43 separat geprüft.
Private Messdateien, SDKs, Caches und Keystore werden nicht veröffentlicht.

