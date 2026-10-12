# B|Cycle 0.7.8 – Dashboard, Fahrtdetails und Rollphasen

Tag `v0.7.8` sichert den vollständigen Quellstand, Tests, Ressourcen, Dokumentation und
signierte Test-APK. Ältere Stände bleiben erhalten. VersionName 0.7.8/VersionCode 46,
MinSdk 33/TargetSdk 35 und identische Signatur mit dem bisherigen Testschlüssel geprüft.

Enthalten sind Dashboard 9 (2 / 3 / 3), feste Dreier-Kachel-Zahlen, Live-Vergleichspfeile,
verbesserter Watt-Durchschnittstrend, 25 Prozent stärkerer/breiterer Glow, korrigierte
FIT-Neuberechnung bei bestätigten Rollphasen, deutsche Fahrtdetails, vertauschte
Kadenz-/Leistungsgruppen, zweistelliges Listenjahr und 3-dp-Versatz der Listenkennzahlen.
Siehe [Dashboard](dashboard-presets.md), [Durchschnitte](ride-averages-0616.md),
[Glow](mode-glow.md), [Performance](performance-energy-0617.md),
[Fahrtdetails](route-analysis-v1.md) und [Streckenliste](route-manager-step2.md).

Vor der letzten kosmetischen Änderung bestanden 307 Tests der vollständigen Suite
und Release-Build (2m 6s); anschließend 43 gezielte UI-Tests/Release-Build (57s).
Die letzte kosmetische Änderung wurde nur mit drei Darstellungstests geprüft.
Nach der reinen APK-Versionierung bestanden diese drei Tests erneut und der
Release-Build einschließlich Release-Lint-Vital in 1m 4s. Keine neue vollständige
Suite, kein vollständiger Lint-Lauf oder Gerätetest für diese Versionierung.
Bekannte Produktionswarnung `onBackPressed()` bleibt bestehen.

APK: 1054393 Byte. SHA-256 `b23dd70ae34f9ba1368f6976ad68a4865479b9b8fd1ab2235009e4548bc9c568`.
Signatur verifiziert; Updateinstallation möglich. Download wird nach Veröffentlichung
von GitHub erneut heruntergeladen und mit der lokalen APK verglichen.
Private FIT-Dateien, personenbezogene Messwerte, Keystore, SDKs und Caches sind nicht Teil des Tags.
