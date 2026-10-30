# APK-Auftrag 0.8.69 / Code 139 – 30.10.2026

Alle Nutzeraufträge umgesetzt und geprüft. APK ausdrücklich angefordert; jetzt über GitHub Actions bauen, prüfen und veröffentlichen. Keine weitere Funktionsänderung ohne neuen Auftrag.

## Stand

- Letzte veröffentlichte APK noch [0.8.68 / Build 109](build-109.md), Quelle vor den Bereinigungen.
- Sicherungstag `vor-den-bereinigungen-2026-10-30` auf GitHub, Quelle ed35551.
- Bereinigung 8000677; Detailzentrierung 9d6ae8c; Test-/Textereste a973a10; Dashboardübergang 896eae5.
- [Änderungen dieser Version](release-0.8.69.md), [Bereinigung](cleanup-2026-10-30.md), [Dashboardwechsel](dashboard-page-transition.md).

## Prüfnachweise

Cleanup-Regressions-/Migrations-/Field-UI-Läufe in `/workspace/.tools/cleanup-*.log`, letzter Profiltest `cleanup-current-profile-test.log` erfolgreich. Unveränderte aktuelle UI-/Sicherungsregeln und alte gültige Daten nachgewiesen. Veraltete Tests auf inzwischen entfernte Bedienelemente angepasst.

Dashboardübergang und Wertzentrierung: `dashboard-transition-verified-tests.log` erfolgreich; nach Lifecycle-Korrektur `dashboard-transition-release-checks.log` erfolgreich (Viewport 5, Editor 2, Detailzentrierung 1). Native Zwischenstandsvorschau visuell geprüft. Gemeinsam getakteter ValueAnimator, Bitmapfreigabe, fester Footer, MapPanel-Identität, Wechsel Karte/ohne Karte und Cancel getestet. Frühere Timingversuche verwendeten automatische Robolectric-Vsync; finale Tests takten den echten Animator gezielt. Kein Produktverhalten nur für Testannahmen geändert.

Keine Gesamtsuite und kein physischer Gerätetest. Source/Versionsänderungen jetzt committed; nächster Schritt Push, passenden Lauf verfolgen, Signatur/Version/Manifest/ZIP-Ausrichtung/SHA-256 prüfen, APK/README/Buildnachweis veröffentlichen, tatsächlichen Raw-Download gegen Artefakt vergleichen.

Alte vollständige Übergabe im [Archiv](archive/CONTINUE-vor-bereinigung-2026-10-30.md). Dortige offene APK-Aufträge sind Vergangenheit.
