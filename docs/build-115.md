# B|CYCLE 0.8.74 – Build 115

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.74-release-115.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.74-release-115.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41202737893)
- Quellstand: `99e76b683dce2155a6c9c4736a49021a68f1843a`
- Version 0.8.74, versionCode144, Android13+.
- Größe: 2564521 Bytes (2,56MB).
- SHA-256: `5ee082bc9ba76c24b2fa630d1e39ff2157a0917f674e9be8e90d41a09f3ffee3`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release aus GitHub Actions. CI-Prüfsumme, Signatur, Version, Manifest ohne Debug-/Test-Flags und ZIP-Ausrichtung geprüft. Dauerhafte Signatur wie Build114; als Update installierbar.

[Änderungen](release-0.8.74.md): Sicherung/Restore bei Bildschirm aus, Datum im HRV-Dateinamen, Intervals-Schlafdauer/Fehlerdiagnose, Footer-Schalter in Fahrradprofilen/Streckendetails, gemeinsame Diagramm-Gestenfläche, TERTIARY-Detailkopf und sparse Bosch-FIT-Kanäle mit echten Lücken.

Prüfungen:51 gezielte Diagramm-/Footer-/FIT-Tests erfolgreich, darunter die lokal bereitgestellte Originaldatei; private Datei und temporärer Test nicht im Repository. Backup-/Restore-/Lifecycle-/Intervals-Prüfungen inklusive 64-Tage-HRV-Export, zuletzt16 gezielte Screen-off-/MapState-/Startfehlerprüfungen erfolgreich. Typografie/Atlas/Diff geprüft. Keine vollständige Gesamtsuite, kein physischer Gerätetest, kein privater Liveupload. Konkreter Intervals-HTTP422 muss erneut geprüft werden.
