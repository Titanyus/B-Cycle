# BICYCLE 0.7.26 – ZIP-Archivimport

Build: 16.10.2026 12:39:06 CEST; versionName 0.7.26, versionCode 64.
Quellstand: `d7a029b3810a352b9e3718ff93e28d2c8d8c04b2`. Tag: `v0.7.26`.

## Änderungen

Strecken → Strecke importieren → ZIP-Archiv importieren. FIT/GPX auch in Unterordnern und als GZIP-Dateien übernehmen, Originalbytes und Fahrtdatum erhalten, identische Dateien anhand des Hashs überspringen. Gedrosselter Fortschritt und sichtbare Abschlussbilanz, begrenzte Verarbeitung einer Datei nach der anderen. Beschädigte Einträge blockieren die folgenden Fahrten nicht; ein beschädigter Archivstrom kann den restlichen Import beenden, bereits gespeicherte Fahrten bleiben erhalten. Aktuelle Dashboard-Strecke bleibt ausgewählt.

Archivimport ausschließlich lokal, ohne automatische Uploads an WebDAV, Intervals.icu, Strava oder Dreeve. Gezielter Massenexport bleibt separat vorgemerkt. TCX-Dateien und CSV-Metadaten werden nicht importiert. Details: [ZIP-Archivimport](route-archive-import.md).

Alle Funktionen aus [0.7.25](milestone-0725.md) bleiben enthalten.

## Verifikation

17 gezielte Tests für Archivimport, FIT-/GPX-Bibliotheken und Streckenliste bestanden, keine Fehler oder übersprungenen Tests. Danach ausschließlich Versionsanhebung und erneuter Release-Build mit R8, Resource-Shrinking und Lint Vital. Keine erneute vollständige Testsuite für diese Veröffentlichung.

Signatur mit bestehendem Testschlüssel, Version/minSdk/targetSdk sowie 16-KiB-ZIP-Ausrichtung geprüft. Kein echtes Strava-Gesamtarchiv am Smartphone getestet. Hardware-Prüfgrenzen der Indoor-Anbindung aus 0.7.25 gelten weiterhin.

## APK

[B-Cycle-0.7.26-test.apk](../downloads/B-Cycle-0.7.26-test.apk)

- 1,616,387 Byte
- SHA-256: `13248de9b0c944a3ca79f276c29ab577ff4a7d7a7ed9401cc7dc085996fbc933`
- Als Update mit bestehendem Android-Testschlüssel installierbar.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
