# B|CYCLE 0.8.91 – Build 133

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.91-release-133.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.91-release-133.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41928772747)
- Quellstand: `48e61a82a21c79c0ad4c8c75e5e3673ca50247f1`
- Version 0.8.91, versionCode 161, Android 13+.
- Größe: 4261296 Bytes (4,26 MB).
- SHA-256: `8294ffb1fa7536e0d237a22a8f7a3fc01e6ffcc0444b1595f4a27de24ab59d85`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 28 Shader und erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Bright-JSON/XML, alle drei eigenen Shader, 111 Spritevarianten und Atlas bytegenau geprüft; Referenzfarben, Dash-Regel und fehlende Extrusionen erhalten. Terrain-Quellenhinweise bytegenau sowie bisherige Editor-/Ebenen-/Fitness-Texte in kompilierten Ressourcen geprüft. Neuer Clipping-Datenpfad und Label-/Editor-/Fitness-/Passklassen im R8-Mapping vorhanden.

[Änderungen](release-0.8.91.md): Maximalzoom-Gebäudegeometrie vor signed-short-Packung auf Zieltile begrenzen; größere radiale Abstände gleicher Straßennummern, viewportweite und lückenorientierte Kandidatenauswahl. Vorhandene Editor-/Fitness-/Profilfunktionen erhalten.

Gezielte native Overzoomprüfung Stufen 15–19 und Quellstufe, bestehende Label-/Fadefälle sowie regelmäßige Verteilung bei umgekehrter Kandidatenreihenfolge und 90° Kartendrehung grün. Typografie/Bright-/Diff-Prüfung grün. Kein Gesamtlauf, keine lokale APK. Gerätevergleich der konkret gemeldeten Szene und realen Straßendichte noch offen.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4261296 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
