# B|CYCLE 0.8.69 – Build 110

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.69-release-110.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.69-release-110.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41061626889)
- Quellstand: `1830fc1ab844e1676be52ce5772e39475b151fe1`
- Version: 0.8.69, versionCode 139, Android 13+.
- Größe: 2531313 Bytes (2,53 MB).
- SHA-256: `0657892cbea304f8f922eaf09a035a5e8c4084ff207c6ecf618fd87f95abb641`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release mit R8/Ressourcenverkleinerung aus GitHub Actions. CI-Prüfsumme, Signatur, Version, Manifest ohne Debug-/Test-Flags und ZIP-Ausrichtung geprüft. Gleiche dauerhafte Signatur wie Build 109; als Update installierbar.

[Änderungen](release-0.8.69.md): Bereinigung überholter Optionen/Zeichenpfade/Sicherungswerte; gemeinsamer 300-ms-Dashboard-/Kartenübergang bei festem Footer; Rahmen-Accordions und gemeinsamer Druckeffekt; Trink- und Feldauswahl; Trenner/Abstände; GPS-lose Höhenzeile und unabhängige Zentrierung der Detailwerte.

Gezielte Native-, Migrations-, Backup- und UI-Prüfungen in mehreren Läufen erfolgreich. Abschließender Übergangslauf: Viewport 5, Editor 2, Detailzentrierung 1. Aktuelle Sensorprofilprüfung zusätzlich erfolgreich. [Bereinigung/Prüfstand](cleanup-2026-10-30.md), [Dashboardübergang](dashboard-page-transition.md). Typografie, UI-Katalog und Diff geprüft. Keine vollständige Gesamtsuite und kein neuer physischer Gerätetest.

Der Stand vor der Bereinigung bleibt als [Git-Tag](https://github.com/Titanyus/B-Cycle/tree/vor-den-bereinigungen-2026-10-30) und [APK Build 109](build-109.md) erhalten.
