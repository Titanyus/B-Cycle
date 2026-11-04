# B|CYCLE 0.8.90 – Build 132

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.90-release-132.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.90-release-132.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41906163906)
- Quellstand: `e52738ee1ca03596786d5ed53ef8645e41205347`
- Version 0.8.90, versionCode 160, Android 13+.
- Größe: 4244912 Bytes (4,24 MB).
- SHA-256: `6187fe34e965f1e0644ffd4b43b9df78fbe4a1f33a6f92645b8429966009388c`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 28 Shader und erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Bright-JSON/XML, alle drei eigenen Shader, 111 Spritevarianten und Atlas bytegenau geprüft; Referenzfarben, Dash-Regel und fehlende Extrusionen erhalten. Terrain-Quellenhinweise bytegenau sowie Editor-/Ebenen-/Fitness-Texte in kompilierten Ressourcen geprüft. Editor, FitnessView und VTM-Passklasse im R8-Mapping vorhanden; alte ChromaticSurface fehlt.

[Änderungen](release-0.8.90.md): Fitness unter Statistiken, getrennte wiederholte Straßen-/Flusslabels, Orte beim Herauszoomen länger behalten, direkter Ebenenbutton, native Farb-/Linienstiländerungen mit JSON-Dateiexport, Höhenlinien unter Straßen/Gebäuden, separater Grenzschalter für admin_level 7 und automatisch abgedunkelte Flat-Sticky-Header.

Gezielte API-/Cache-/Diagramm-, Label-/Fade-, Popup-/Editor-/Profil-/Backup-/Reihenfolge-/Grenzen- und Flat-Farbprüfungen erfolgreich. Tatsächlicher nativer Dateipicker samt UTF-8-Ausgabe geprüft. Typografie/UI-Katalog/Bright-/Diff-Prüfung grün. Kein Gesamtlauf, keine lokale APK. Gerätebild für Dash-Muster/Überdeckung/Zoom und Live-Abgleich mit dem Intervals-Konto noch zu beurteilen.
