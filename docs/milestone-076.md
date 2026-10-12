# B|Cycle 0.7.6 – Fahrerprofil und Karvonen-Zonen

Tag `v0.7.6` sichert Quellcode, Tests, Ressourcen, Dokumentation und die signierte Test-APK. Vorherige Stände und Downloads bleiben erhalten.

Änderungen: [Fahrerprofil](rider-profile.md), [vier Spalten im Streckenmanager und vereinheitlichte Headerhöhe](route-manager-step2.md).

Vollständige Testsuite vor der reinen APK-Versionierung: 280 Tests bestanden, 0 Fehler, 0 Fehlschläge, 0 übersprungen; Release-Build in 2m. Anschließend VersionName 0.7.6/VersionCode 44 gesetzt: 19 gezielte Profil-/Karvonen-/FIT-Tests erneut bestanden und Release-Build in 53s. Keine erneute vollständige Suite nach dieser Versionsänderung.
Release-Lint-Vital erfolgreich. Bekannte Produktionswarnung `onBackPressed()` bleibt. Kein zusätzlicher vollständiger Lint-Lauf oder Gerätetest.

APK: 1021457 Byte. SHA-256 `7c0d6939f4b1b195fc11e08062cdad36619cd2e8faf0c696c92b6b88510382a6`.
Signatur separat geprüft und identisch mit dem bisherigen Android-Testschlüssel; als Update installierbar. MinSdk 33, TargetSdk 35 und Version 0.7.6/44 geprüft.

Zu prüfen am Telefon: Fahrerprofil im sechsten Einstellungsplatz, Date-Picker/Locale/Tastatur, Live-Zonen, Abbrechen/Zurück und Speicherung, vier Spalten der Streckenliste, weggefallener Verarbeitungshinweis und einheitliche Headerhöhe. Keine automatische Profilübertragung oder Änderungen an FIT-Rohdaten. Keine neuen Trainingsanalysen.
Private Messdateien, SDKs, Caches und Keystore werden nicht veröffentlicht.
