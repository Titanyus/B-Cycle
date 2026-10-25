# B|CYCLE 0.8.43 – Build 83

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.43-release-83.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.43-release-83.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40176184882)
- Quellstand: `a657e39bcf3031ad4425b50c1c112924df1c1c86`
- Version: 0.8.43, versionCode113, Android13+.
- Größe: 2.425.717 Bytes (2,43 MB).
- SHA-256: `dd177b8d53f7292002226462603b6790a3d268475e82bb1c540a8681a59ff640`
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release aus GitHub Actions mit R8 und Ressourcenverkleinerung. CI-Prüfsumme, dauerhafte Signatur wie Build82, Version/Manifest ohne Debug-/Test-Flag und ZIP-Ausrichtung geprüft. Direkter APK-Download ohne ZIP; Update über gleich signierte Vorgänger seit Build53 möglich.

Allgemeine Dialogumstellung aus Build82 zurückgenommen: ursprüngliche Header-/Seitenfarben wiederhergestellt, ebenso HRV-Tageskarte und vorhandene HRV-/Fahrtabschluss-Ausnahmen. PRIMARY-Trinkhinweis ohne Header und separate Glow-Sticky-Karten erhalten. [Alle Änderungen](release-0.8.43.md).

31 gezielte Tests erfolgreich: ursprüngliche Rollen, Alert/Trinkdauer/-position, Theme/Flat/Glow, HRV-Einstieg, kompakte Dialogaktionen, Vollbild/Insets/Statusleiste sowie Sticky-Karten. Native Vorschau visuell geprüft. Typografie-/Katalog-/Diffchecks erfolgreich. Kein vollständiger Gesamtlauf oder Hardwaretest. Frühere unveränderte UI-Testfehler im [Vorgänger-Buildnachweis](build-82.md), nicht Teil des gezielten Laufs. Seither nur Version und Release-Dokumentation geändert; keine unnötige Testwiederholung. [Vorheriger Download](build-82.md).
