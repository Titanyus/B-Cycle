# B|CYCLE 0.8.42 – Build 82

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.42-release-82.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.42-release-82.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40173060582)
- Quellstand: `3ed8d66bd715c95c38ba4a9a3f79cda23c0e51ac`
- Version: 0.8.42, versionCode112, Android13+.
- Größe: 2.426.625 Bytes (2,43 MB).
- SHA-256: `a67a87cc56befc71aa1f7b9e6883c09f7221d2f23a17a2c62ce0de20e411acd0`
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release aus GitHub Actions mit R8 und Ressourcenverkleinerung. CI-Prüfsumme, dauerhafte Signatur wie Build81, Version/Manifest ohne Debug-/Test-Flag und ZIP-Ausrichtung geprüft. Direkter APK-Download ohne ZIP; Update über gleich signierte Vorgänger seit Build53 möglich.

Enthält Glow-Sticky-Karten, gemeinsame TERTIARY-Dialogbasis und PRIMARY-Trink-Hinweis sowie alle lokalen Nachträge seit Build81: dezentes Preset9, Pillen-Slider, stabile FIT-Sticky-Schriftrolle und weicher HRV-Puls in beiden Designs. [Alle Änderungen](release-0.8.42.md).

Neue native Glow-/Flat-/Theme-/Aufklapp-/Sticky-/Spacing-Prüfungen erfolgreich, echte Fahrerprofiltests erfolgreich, native Karten-Vorschau visuell geprüft. Vorherige gezielte Dialog-/HRV-/Trink-, Preset-/Blur- und Sliderprüfungen ebenfalls erfolgreich. Typografie-/Katalog-/Designer-/Diff-Prüfungen erfolgreich. Kein vollständiger Gesamtlauf oder Hardwaretest.

Drei ältere UI-Testfehler bleiben unverändert und wurden am direkten Vorgängerstand1a64a50 separat reproduziert: StickyAccordionSettingsTest51 (Spinnerlayout),129 (alte PopupSECONDARY-Erwartung), ProfileAccordionTest53 (alte Strong-Schrifterwartung). Identische Fehlerstellen vor und nach dieser Änderung; keine Tests deaktiviert. Native diffuse Dialogschatten nicht auf echter GPU geprüft. [Vorheriger Download](build-81.md).
