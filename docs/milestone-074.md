# B|Cycle 0.7.4 – Estimated FTP und Details-Update

Tag `v0.7.4` sichert den vollständigen Quellcode, Tests, Ressourcen, Dokumentation und die signierte Test-APK. Vorherige Stände bleiben erhalten.

Änderungen und Datenregeln: [Estimated FTP und Streckendetails](estimated-ftp.md).
Vollständige Testsuite und Release-Build für das Funktionsupdate: 259 Tests bestanden, 0 Fehler, 0 Fehlschläge, 0 übersprungen, Laufzeit 1m 15s.
Danach ausschließlich APK-Versionierung auf 0.7.4/42: 6 FIT-Tests erneut bestanden, Release-Build erfolgreich in 33s. Keine erneute vollständige Suite nach dieser Versionsänderung.
Release-Lint-Vital erfolgreich. Nur die bekannte Produktionswarnung `onBackPressed()` bleibt. Kein zusätzlicher vollständiger Lint-Lauf oder Gerätetest.

APK: 996685 Byte. SHA-256 `eb89b21325cba4a4af71dd91e59bec2dc80d3672251090f7688bd37dfe80d5dd`.
Signatur geprüft und identisch mit dem bisherigen Testschlüssel, als Update installierbar.
MinSdk 33, TargetSdk 35, VersionName 0.7.4, VersionCode 42 separat geprüft.

Zu prüfen am Telefon: Vollbildkarte/Interaktion, Daten und Einheiten in den Details, E-Bike-Auswertung und Estimated FTP mit mindestens 20 Minuten geeigneter Leistungsdaten. Private Messdateien, SDKs, Caches und Keystore werden nicht veröffentlicht.
