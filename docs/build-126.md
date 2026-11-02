# B|CYCLE 0.8.85 – Build 126

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.85-release-126.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.85-release-126.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41526442507)
- Quellstand: `73dae6f21d7339e31c2f6d3e5cbb9e89e9dc9279`
- Version 0.8.85, versionCode 155, Android 13+.
- Größe: 4204773 Bytes (4,20 MB).
- SHA-256: `adb2bd9e24b4ca51fa2ced9af061e8687634448a6ac7a30b82f0155946d2646d`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 27 Shader sowie erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Unveränderte Bright-JSON, generierte XML, eigene GPS-/Streckenshader, 111 referenzierte Sprite-/Patternvarianten und Originalatlas/Metadaten bytegenau geprüft. Referenzfarben/Dash-Regel, fehlende Extrusionen, Lizenzen/OpenMapTiles-Ressourcen und vorhandene Aufzeichnungs-/Trainingshilfe-Texte in kompilierten Ressourcen geprüft.

[Änderung](release-0.8.85.md): dauerhafter DashboardMapHost statt Umhängen der GL-Karte beim Dashboardwechsel. Slots bestimmen Position und Zielgröße; Footerbezug und auslaufende Kartenaufnahme erhalten. [Technik](dashboard-page-transition.md).

Vier gezielte Fälle erfolgreich: Vektorkarte mit beiden Footerzuständen/Abbruch/Commit (0 Attach/Detach, gleiches Host-/Layer-/Theme-Objekt), kartenlose Vorschau, echte Dashboardintegration, Kartenpixel in auslaufender Softwareaufnahme und Bitmapfreigabe. Kompilation/Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf und keine lokale APK. Echte Geräte-GPU und Zoom-/Text-/Kacheldarstellung nach Update noch bestätigen.
