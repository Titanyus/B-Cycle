# B|CYCLE 0.8.83 – Build 124

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.83-release-124.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.83-release-124.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41481660464)
- Quellstand: `f088f6bdc9f95ced2c18643bfc33cfe705034115`
- Version 0.8.83, versionCode 153, Android 13+.
- Größe: 4204369 Bytes (4,20 MB).
- SHA-256: `ba5fdf923684083fe0f6c067de96b8a84ba1567c81377b1643e07ae2e308e69d`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 27 Shader sowie erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Unveränderte Bright-Referenz-JSON in der APK mit SHA-256 cda8931095c5101cbad5d2a60fa129600a1a0ad3dea27d802be341fb5eae071f nachgewiesen. Generierte XML und eigene GPS-/Streckenshader binär mit Quelle verglichen. Alle 111 in der XML referenzierten Sprite-/Patternvarianten sowie Originalatlas/Metadaten bytegenau geprüft. Referenzfarben für Hintergrund, Wald, Gras und Wasser, echte Dash-Regel und fehlende Extrusionen geprüft. Bright-Lizenz/OpenMapTiles-Ressourcen vorhanden.

[Änderungen](release-0.8.83.md), [vollständige Zuordnung und Grenzen](vtm-bright-style.md). Vier gezielte Komponenten-/Daten-/Linien-/Quellenwechseltests erfolgreich, nach Ergänzung nur zwei betroffene Fälle wiederholt. Generator-Reproduzierbarkeit, Kompilation, Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf und keine lokale APK. Physischer Vergleich mit Maputnik noch offen; keine pixelgleiche MapLibre-Darstellung behaupten.
