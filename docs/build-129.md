# B|CYCLE 0.8.88 – Build 129

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.88-release-129.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.88-release-129.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41880129963)
- Quellstand: `07b8d5b94a566d46e02ff793ec13fd488b50efd0`
- Version 0.8.88, versionCode 158, Android 13+.
- Größe: 4189008 Bytes (4,19 MB).
- SHA-256: `947936351d20fc760351fe11b88a7be328850470c0a3b926bcb828ac33421fbe`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, jetzt 28 Shader und erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Bright-JSON/XML, GPS-/Streckenshader und neuer bcycle_labels-Shader, 111 Spritevarianten und Atlas bytegenau geprüft; Referenzfarben, Dash-Regel, fehlende Extrusionen und Track-/HRV-/Dashboardtexte in kompilierten Ressourcen geprüft. Lokale BCycleLabelPlacement-Anpassung in R8-Mapping vorhanden; alte BicycleChromaticSurface fehlt.

[Änderungen](release-0.8.88.md): weniger Ortsnamen, Zoomkohorten mit Hysterese, stabile gleichklassige Namen, 240-ms-GPU-Fades, ausgeblendete Gemeindegrenzen, korrigierte TERTIARY-Feldauswahl mit globalem 16-dp-Abstand und 8-dp-Seiteninset der Glow-Sticky-Kästen.

Acht gezielte native Fälle für Platzierung/Fades, Grenzen, Vollbild-Feldauswahl, Boxgeometrie und Layerwechsel erfolgreich. GLES-Shader-Compile/Link und Alpha-Pixel für Ein-/Ausblendung/Umkehr geprüft. Kompilation/Typografie/Atlas/Bright-/Diff-Prüfung grün; kein Gesamtlauf, keine lokale APK. Smartphone-GPU und subjektiver Zoom-Eindruck noch zu beurteilen.
