# B|CYCLE 0.8.84 – Build 125

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.84-release-125.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.84-release-125.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41523444594)
- Quellstand: `daabf8c8aa69fd0b3e2db8f3a9303a474f622f71`
- Version 0.8.84, versionCode 154, Android 13+.
- Größe: 4204773 Bytes (4,20 MB).
- SHA-256: `eeeb19933205f7f11a4c04f07249951e37fb69abab9fc7d3ffa3787568a67bf4`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 27 Shader sowie erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Originale Bright-JSON mit SHA-256 cda8931095c5101cbad5d2a60fa129600a1a0ad3dea27d802be341fb5eae071f in der APK geprüft. Generierte XML, eigene GPS-/Streckenshader, alle 111 referenzierten Sprite-/Patternvarianten und Originalatlas/Metadaten bytegenau mit Quelle verglichen. Referenzfarben, Dash-Regel, fehlende Extrusionen, Bright-Lizenz und OpenMapTiles-Ressourcen vorhanden. Neue Aufzeichnungsüberschriften/-beschreibung und Trainingshilfe-Text in den kompilierten Ressourcen nachgewiesen.

[Änderungen](release-0.8.84.md): tatsächlicher Bright-GL-Hintergrund, echte Kartenvorschau mit passendem Footer, gemeinsame Aufzeichnung in Fahrradprofilen, 8-dp-Dropdown-Abstände und neuer Trainingshilfe-Text.

Acht eindeutige gezielte Fälle erfolgreich (Hintergrund/Quellenwechsel, Dashboardübergang/Abbruch, Profilanordnung/Quellenwahl/Sicherung). Nach Ergänzungen nur betroffene/offene Fälle wiederholt; Textänderung separat per Ressourcenprüfung. Generator/Kompilation/Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf und keine lokale APK. Optischen Kartenvergleich und GL-Neuanschluss/Übergang am Smartphone noch prüfen.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4204773 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
