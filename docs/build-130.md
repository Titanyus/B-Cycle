# B|CYCLE 0.8.89 – Build 130

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.89-release-130.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.89-release-130.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41898829728)
- Quellstand: `fb5438bdc7ed0b2c34b821543821c8267afc2817`
- Version 0.8.89, versionCode 159, Android 13+.
- Größe: 4207496 Bytes (4,21 MB).
- SHA-256: `53cf66eaee31ddef5ada8f995072f1743da8bbf5c2c703d71154488580c67eb6`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 28 Shader und erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Bright-JSON/XML, alle drei eigenen Shader, 111 Spritevarianten und Atlas bytegenau geprüft; Referenzfarben, Dash-Regel und fehlende Extrusionen erhalten. Terrain-Quellenhinweise bytegenau sowie neue Ebenen-/Höhenlinien-Texte in kompilierten Ressourcen geprüft. Lokale Label- und Terrain-Klassen im R8-Mapping vorhanden; alte ChromaticSurface fehlt.

[Änderungen](release-0.8.89.md): fehlende Glyphen durch falsche GL-Attributaktivierung korrigiert, Straßennummer und Hintergrund gekoppelt, Kartenebenen je Profil schaltbar, optionale Höhenlinien aus Mapzen/AWS Terrain.

Drei gezielte Beschriftungs-/Schildprüfungen und drei Layer-/Profil-/Terrain-/UI-Prüfungen erfolgreich. Echte GLES-Pixelprüfung mit vier erzwungenen Attributbelegungen reproduziert den alten Glyphenfehler und bestätigt die Korrektur. Reale Terrainprobe plausibel; Typografie/UI-Katalog/Bright-/Diff-Prüfung grün. Kein Gesamtlauf, keine lokale APK. Smartphone-GPU und Zoom-/Terrain-Lesbarkeit noch zu beurteilen.
