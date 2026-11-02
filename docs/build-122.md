# B|CYCLE 0.8.81 – Build 122

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.81-release-122.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.81-release-122.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41462180674)
- Quellstand: `6ecbd52ae58a0296fe15d08348e237a7cfe79ba2`
- Version 0.8.81, versionCode 151, Android 13+.
- Größe: 4075040 Bytes (4,08 MB).
- SHA-256: `8301a0ac5a7a88f55157deda4aeba35db37b08517c9989c3afce042788ad3b0e`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release aus GitHub Actions. CI-Prüfsumme, Version, Manifest ohne Debug-/Test-Flags und dauerhafte Signatur geprüft. Update-Zertifikat wie Build 121. ZIP-Ausrichtung einschließlich 16-KB-Ausrichtung nativer Bibliotheken geprüft; ARM64-ELF-LOAD-Segmente mindestens 16-KB-ausgerichtet. Vier VTM-ABIs und 27 Shader vorhanden, einschließlich GPS- und Streckenshader. Bright-XML und beide eigenen Shader binär gegen Quelle verglichen; Bright-Hintergrund und fehlende Extrusionen in der APK geprüft. OpenMapTiles-Ressourcen/Bright-Lizenz, native GLMatrix-Bezeichnung und protobuf-MVT-Namen nach R8 vorhanden.

[Änderungen](release-0.8.81.md): Akkusparmodus je Fahrradprofil, Bright in Draufsicht, Standort/Höhe im GL-Kartenframe, stabile Live-Strecke, sichtbare Nachbarseite beim Ziehen, achtstufiger Stressslider und FOREGROUND-Farbauswahlrahmen.

Gezielte Komponenten-/Integrations-/Backup-/Aufzeichnungstaktprüfungen je Änderung erfolgreich; GPS-/Streckenshader zusätzlich mit echtem lokalem Mesa/GLES geprüft. Nur betroffene/offene Fälle nach Ergänzungen wiederholt, kein Gesamtlauf und keine lokale APK. Typografie/Atlas/Diff erfolgreich. Tatsächliche Android-GPU-Ausgabe, Snapshot/Umhängen verschiedener Kartenhöhen, Wischgefühl und Akkueffekt am Smartphone noch bestätigen. Keine gemessene Prozentersparnis. Minimal-/OLED-Stil und Live-Stilimport nicht enthalten.
