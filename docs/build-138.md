# B|CYCLE 0.8.95 – Build 138

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.95-release-138.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.95-release-138.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/42046418022)
- Quellstand: `9ea2eccca0bd9ea030b1c909491291ffa8d96f92`
- Version 0.8.95, versionCode 165, Android 13+.
- Größe: 4279004 Bytes (4,28 MB).
- SHA-256: `7dce365074092c1f2ade1b7f2dacdc37c20dbcbbc3ee2c48058f12210b1c8b95`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 28 Shader und erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Bright-JSON/XML, drei eigene Shader, 111 Spritevarianten und Atlas bytegenau geprüft; Referenzfarben/Dash-Regel und fehlende Extrusionen erhalten. Terrain- und Cycling-Quellenhinweise bytegenau. Karten-/Designer-/Cycling-Texte und R8-Namen einschließlich neuem Radroutenlayer/-datenpfad und zweizeiligem Caption-Layout geprüft.

[Änderungen](release-0.8.95.md): eigene Radwege-/Radrouten-Optionen samt Trackstilreglern, echter optionaler Radrouten-Vektorlayer für die Übersicht, eindeutige Feldweg-Kategorien und zweizeilige lange Ortsnamen. JSON enthält Darstellungen, Schriftparameter und Ebenenauswahl.

Gezielte Prüfungen grün: drei Ortsnamen-/Glyphenpixel-/Platzierungs-/Schilder-/Exportfälle, vier Cycling-/Native-/Profil-/Backup-/JSON-/Cache-/Farb-/Trackfälle und ein echter Karten-Einstellungs-/Raster-/Vektor-Integrationstest. Öffentliche Übersicht und Detailquelle tatsächlich abgerufen. Typografie/UI-Katalog/Bright-/Diff-Prüfung erfolgreich. Kein Gesamtlauf und keine lokale APK. Optische Geräteprüfung offen; einzelne physische Radwege bleiben an Quellzoomdetails gebunden, sehr weite Radroutenansichten priorisieren bis100 größere Routen pro Kachel.

Downloadnachweis: Den veröffentlichten direkten Raw-Link abgerufen: 4279004 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
