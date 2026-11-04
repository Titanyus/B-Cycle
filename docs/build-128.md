# B|CYCLE 0.8.87 – Build 128

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.87-release-128.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.87-release-128.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41829913611)
- Quellstand: `d60be354990fd63f1de57f6666ab9b74751f2f80`
- Version 0.8.87, versionCode 157, Android 13+.
- Größe: 4188317 Bytes (4,19 MB).
- SHA-256: `1ac96e326420a15ea4980253743ae6dba8b31b87ac92681555c1ea24aacdd9f8`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 27 Shader und erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Bright-JSON/XML, GPS-/Streckenshader, 111 Spritevarianten und Atlas unverändert bytegenau geprüft; Referenzfarben, Dash-Regel, fehlende Extrusionen und Track-/HRV-/Dashboardtexte in kompilierten Ressourcen geprüft. Lokale BCycleLabelPlacement-Anpassung in R8-Mapping vorhanden; alte BicycleChromaticSurface fehlt.

[Änderungen](release-0.8.87.md): einfarbige Controls, SECONDARY-Dropdowns, Track-Iconfarben, vollständiger Streckentitel, Vektorkarte ohne POIs, priorisierte/stabile Kartennamen, TERTIARY-HRV-Karten und Feldbelegungsflächen. Alte dekorative Engine im [Wiederverwendungsarchiv](archive/chromatic-glow-2026-11-04/README.md) erhalten.

Gezielte Prüfungen über die einzelnen Aufträge erfolgreich. Zuletzt sechs Fälle: echte VTM-Platzierung über Tile-/Zoom-/Stilwechsel, Orts-/Straßenprioritäten, Layerwechsel, HRV-Einstellungen/Sicherungsrundreise/Flächen und Feldbelegung. Kompilation/Typografie/Atlas/Bright-Generator/Diff grün; kein Gesamtlauf, keine lokale APK. Gerätesichtprüfung, besonders die Beschriftungsruhe beim Pinch-Zoom, steht aus.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4188317 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
